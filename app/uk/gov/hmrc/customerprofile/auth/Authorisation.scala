/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customerprofile.auth

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result, Results}
import uk.gov.hmrc.api.controllers.{ErrorAcceptHeaderInvalid, ErrorResponse, ErrorUnauthorizedLowCL, HeaderValidator}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{confidenceLevel, email, nino}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.customerprofile.controllers.{AccountWithLowCL, ErrorUnauthorizedNoNino, ErrorUnauthorizedUpstream, FailToMatchTaxIdOnAuth, ForbiddenAccess, NinoNotFoundOnAccount}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait Authorisation extends Results with AuthorisedFunctions {

  val confLevel: Int
  val logger: Logger = Logger(this.getClass)

  lazy val requiresAuth: Boolean = true
  lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount("The user must have a National Insurance Number")
  lazy val failedToMatchNino     = new FailToMatchTaxIdOnAuth("The nino in the URL failed to match auth!")
  lazy val lowConfidenceLevel    = new AccountWithLowCL("Unauthorised! Account with low CL!")

  def grantAccess(
    requestedNino: Option[Nino]
  )(implicit hc:   HeaderCarrier,
    ec:            ExecutionContext
  ): Future[Unit] =
    if (requestedNino.isDefined) {
      val suppliedNino = requestedNino.getOrElse(throw ninoNotFoundOnAccount)
      authorised(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", suppliedNino.nino)), "Activated", None))
        .retrieve(nino and confidenceLevel) {
          case Some(foundNino) ~ foundConfidenceLevel =>
            if (foundNino.isEmpty) throw ninoNotFoundOnAccount
            if (!foundNino.equals(suppliedNino.nino)) throw failedToMatchNino
            if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
            Future successful ()
          case None ~ _ => throw ninoNotFoundOnAccount
        }

    } else {
      authorised().retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel =>
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
          Future successful ()
        case None ~ _ => throw ninoNotFoundOnAccount
      }
    }

  def invokeAuthBlock[A](
    request:     Request[A],
    block:       Request[A] => Future[Result],
    taxId:       Option[Nino]
  )(implicit ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    grantAccess(taxId)
      .flatMap { _ =>
        block(request)
      }
      .recover {
        case _: NinoNotFoundOnAccount =>
          logger.info("Unauthorized! NINO not found on account!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedNoNino))

        case _: FailToMatchTaxIdOnAuth =>
          logger.info("Forbidden! Failure to match URL NINO against Auth NINO")
          Forbidden(Json.toJson[ErrorResponse](ForbiddenAccess))

        case _: AccountWithLowCL =>
          logger.info("Unauthorized! Account with low CL!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedLowCL))

        case ex: UpstreamErrorResponse if (ex.statusCode < 500) =>
          logger.info("Unauthorized! Failed to grant access since 4xx response!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedUpstream))
      }
  }
}

trait AccessControl extends HeaderValidator with Authorisation {
  outer =>
  def parser: BodyParser[AnyContent]

  def validateAcceptWithAuth(
    rules:       Option[String] => Boolean,
    taxId:       Option[Nino]
  )(implicit ec: ExecutionContext
  ): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser:                     BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext       = outer.executionContext

      def invokeBlock[A](
        request: Request[A],
        block:   Request[A] => Future[Result]
      ): Future[Result] =
        if (rules(request.headers.get("Accept"))) {
          if (requiresAuth) invokeAuthBlock(request, block, taxId)
          else block(request)
        } else
          Future.successful(
            Status(ErrorAcceptHeaderInvalid.httpStatusCode)(Json.toJson[ErrorResponse](ErrorAcceptHeaderInvalid))
          )
    }

}
