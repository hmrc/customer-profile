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

package uk.gov.hmrc.customerprofile.controllers

import com.google.inject.{Inject, Singleton}
import play.api.Logger

import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.{Action, ActionBuilder, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.customerprofile.auth._
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.services.ApplePassService
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest
import uk.gov.hmrc.domain.Nino

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplePassController @Inject()(
                                     service:                                          ApplePassService,
                                     accessControl:                                    AccountAccessControl,
                                     controllerComponents:                             ControllerComponents,
                                     shutteringConnector:                              ShutteringConnector
)(implicit val executionContext                :ExecutionContext)
    extends BackendController(controllerComponents)
    with HeaderValidator with ErrorHandling with ControllerChecks {
  outer =>
  override val logger: Logger = Logger(this.getClass)
   def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
   val app                          = "Apple Pass Controller"

  def invokeAuthBlock[A](
    request: Request[A],
    block: Request[A] => Future[Result],
    taxId: Option[Nino]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = fromRequest(request)

    accessControl
      .grantAccess(taxId)
      .flatMap { _ =>
        block(request)
      }
      .recover {
        case _: Upstream4xxResponse =>
          logger.info("Unauthorized! Failed to grant access since 4xx response!")
          Unauthorized(toJson[ErrorResponse](ErrorUnauthorizedMicroService))

        case _: NinoNotFoundOnAccount =>
          logger.info("Unauthorized! NINO not found on account!")
          Forbidden(toJson[ErrorResponse](ErrorUnauthorizedNoNino))

        case _: FailToMatchTaxIdOnAuth =>
          logger.info("Unauthorized! Failure to match URL NINO against Auth NINO")
          Forbidden(toJson[ErrorResponse](ErrorUnauthorized))

        case _: AccountWithLowCL =>
          logger.info("Unauthorized! Account with low CL!")
          Forbidden(toJson[ErrorResponse](ErrorUnauthorizedLowCL))

        case e: AuthorisationException =>
          Unauthorized(obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))
      }
  }
       def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request, AnyContent] =
         new ActionBuilder[Request, AnyContent] {

           def invokeBlock[A](
            request: Request[A],
            block:   Request[A] => Future[Result]
          ): Future[Result] =
            if (acceptHeaderValidationRules(request.headers.get("Accept"))) {
          invokeAuthBlock(request, block, taxId)
        } else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson[ErrorResponse](ErrorAcceptHeaderInvalid)))
      override def parser:                     BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext       = outer.executionContext
    }

  def getApplePass(journeyId: JourneyId): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive().async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
              service
                .getApplePass()
                .map(response => Ok(toJson(response)))
          }
        }
      }
    }
}
