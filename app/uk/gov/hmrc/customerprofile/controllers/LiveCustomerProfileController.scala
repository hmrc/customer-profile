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
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.customerprofile.auth._
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.domain.{ChangeEmail, Paperless, PaperlessOptOut, TermsAccepted}
import uk.gov.hmrc.customerprofile.services.CustomerProfileService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiveCustomerProfileController @Inject() (
  service:                                                     CustomerProfileService,
  accessControl:                                               AccountAccessControl,
  @Named("citizen-details.enabled") val citizenDetailsEnabled: Boolean,
  controllerComponents:                                        ControllerComponents,
  shutteringConnector:                                         ShutteringConnector,
  @Named("optInVersionsEnabled") val optInVersionsEnabled:     Boolean
)(implicit val executionContext:                               ExecutionContext)
    extends BackendController(controllerComponents)
    with CustomerProfileController with ErrorHandling with ControllerChecks {
  outer =>
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override val logger: Logger = Logger(this.getClass)

  val app                           = "Live-Customer-Profile"

  def invokeAuthBlock[A](
    request: Request[A],
    block:   Request[A] => Future[Result],
    taxId:   Option[Nino]
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

  override def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request, AnyContent] =
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

  override def getPersonalDetails(
    nino:      Nino,
    journeyId: JourneyId
  ): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive(Some(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            if (citizenDetailsEnabled) {
              service
                .getPersonalDetails(nino)
                .map(as => Ok(toJson(as)))
                .recover {
                  case Upstream4xxResponse(_, LOCKED, _, _) =>
                    result(ErrorManualCorrespondenceIndicator)
                }
            } else Future successful result(ErrorNotFound)
          }
        }
      }
    }

  override def getPreferences(journeyId: JourneyId): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive().async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper(
            service.getPreferences().map {
              case Some(response) => Ok(toJson(service.reOptInEnabledCheck(response)))
              case _              => NotFound
            }
          )
        }
      }
    }

  override def optIn(
    settings:    Paperless,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    request:     Request[_]
  ): Future[Result] =
    shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
      withShuttering(shuttered) {
        val settingsToSend =
          if (!optInVersionsEnabled) settings.copy(generic = settings.generic.copy(optInPage = None)) else settings
        errorWrapper(service.paperlessSettings(settingsToSend, journeyId).map {
          case PreferencesExists | EmailUpdateOk => NoContent
          case PreferencesCreated                => Created
          case EmailNotExist                     => Conflict(toJson[ErrorResponse](ErrorPreferenceConflict))
          case NoPreferenceExists                => NotFound(toJson[ErrorResponse](ErrorNotFound))
          case _                                 => InternalServerError(toJson[ErrorResponse]((PreferencesSettingsError)))
        })
      }
    }

  override def optOut(
    paperlessOptOut: PaperlessOptOut,
    journeyId:       JourneyId
  )(implicit hc:     HeaderCarrier,
    request:         Request[_]
  ): Future[Result] =
    shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
      withShuttering(shuttered) {
        val paperlessOptOutToSend =
          if (!optInVersionsEnabled)
            paperlessOptOut.copy(generic =
              Some(paperlessOptOut.generic.getOrElse(TermsAccepted(Some(false))).copy(optInPage = None))
            )
          else paperlessOptOut
        errorWrapper(service.paperlessSettingsOptOut(paperlessOptOutToSend).map {
          case PreferencesExists       => NoContent
          case PreferencesCreated      => Created
          case PreferencesDoesNotExist => NotFound
          case _                       => InternalServerError(toJson[ErrorResponse](PreferencesSettingsError))
        })
      }
    }

  override def pendingEmail(
    changeEmail: ChangeEmail,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    request:     Request[_]
  ): Future[Result] =
    shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
      withShuttering(shuttered) {
        errorWrapper(service.setPreferencesPendingEmail(changeEmail).map {
          case EmailUpdateOk      => NoContent
          case EmailNotExist      => Conflict
          case NoPreferenceExists => NotFound
          case _                  => InternalServerError(toJson[ErrorResponse](PreferencesSettingsError))
        })
      }
    }

}
