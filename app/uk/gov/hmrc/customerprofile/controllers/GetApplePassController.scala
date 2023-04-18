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

import javax.inject.Named
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.{Action, ActionBuilder, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.customerprofile.auth._
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.services.GetApplePassService
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetApplePassController @Inject()(
                                     service : GetApplePassService,
                                     accessControl: AccountAccessControl,
                                     @Named("citizen-details.enabled") val citizenDetailsEnabled: Boolean,
                                     controllerComponents:  ControllerComponents,
                                     shutteringConnector: ShutteringConnector
                                   )
                                      (implicit val executionContext:ExecutionContext)
  extends BackendController(controllerComponents)  with HeaderValidator
     {
  outer =>

       val logger: Logger = Logger(this.getClass)
   def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  private final val WebServerIsDown = new Status(521)
  val app = "Live-Customer-Profile"

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

      def invokeBlock[A](request: Request[A],
                          block: Request[A] => Future[Result]
                        ): Future[Result] =
        if (acceptHeaderValidationRules(request.headers.get("Accept"))) {
          invokeAuthBlock(request, block, taxId)
        } else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson[ErrorResponse](ErrorAcceptHeaderInvalid)))
      override def parser: BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext = outer.executionContext
    }

   def withShuttering(shuttering: Shuttering)(fn: => Future[Result]): Future[Result] =
    if (shuttering.shuttered) Future.successful(WebServerIsDown(Json.toJson(shuttering))) else fn

  def log(message: String): Unit = logger.info(s"$app $message")

  def result(errorResponse: ErrorResponse): Result =
    Status(errorResponse.httpStatusCode)(toJson(errorResponse))

  def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    func.recover {
      case e: AuthorisationException =>
        Unauthorized(obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))

      case _: NotFoundException =>
        log("Resource not found!")
        result(ErrorNotFound)

      case _: NinoNotFoundOnAccount =>
        log("User has no NINO. Unauthorized!")
        Forbidden(toJson[ErrorResponse](ErrorUnauthorizedNoNino))

      case e: Throwable =>
        logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson[ErrorResponse](ErrorInternalServerError))
    }

  def getApplePass(journeyId: JourneyId): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive().async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            if (citizenDetailsEnabled) {
              service
                .getApplePass()
                .map(response => Ok(toJson(response)))
            } else Future successful result(ErrorNotFound)
          }
        }
      }
    }

}
