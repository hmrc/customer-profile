/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.JsValue

import javax.inject.Named
import play.api.libs.json.Json.toJson
import play.api.mvc.*
import uk.gov.hmrc.api.controllers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.connector.*
import uk.gov.hmrc.customerprofile.domain.types.JourneyId
import uk.gov.hmrc.customerprofile.domain.{ChangeEmail, Paperless, PaperlessOptOut, TermsAccepted}
import uk.gov.hmrc.customerprofile.services.CustomerProfileService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiveCustomerProfileController @Inject() (
  override val authConnector: AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  service: CustomerProfileService,
  @Named("citizen-details.enabled") val citizenDetailsEnabled: Boolean,
  controllerComponents: ControllerComponents,
  shutteringConnector: ShutteringConnector,
  @Named("optInVersionsEnabled") val optInVersionsEnabled: Boolean
)(implicit val executionContext: ExecutionContext)
    extends BackendController(controllerComponents)
    with CustomerProfileController
    with ErrorHandling
    with ControllerChecks {
  outer =>
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override val logger: Logger = Logger(this.getClass)

  val app = "Live-Customer-Profile"

  override def getPersonalDetails(
    nino: Nino,
    journeyId: JourneyId
  ): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Some(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            if (citizenDetailsEnabled) {
              service
                .getPersonalDetails(nino)
                .map(as => Ok(toJson(as)))
            } else Future successful result(ErrorNotFound)
          }
        }
      }
    }

  override def getPreferences(journeyId: JourneyId): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async { implicit request =>
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

  override def paperlessSettingsOptIn(journeyId: JourneyId): Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async(controllerComponents.parsers.json) { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          request.body
            .validate[Paperless]
            .fold(
              errors => {
                logger.warn("Received error with service getPaperlessSettings: " + errors)
                Future successful BadRequest
              },
              settings => {
                val settingsToSend =
                  if (!optInVersionsEnabled) settings.copy(generic = settings.generic.copy(optInPage = None))
                  else settings
                errorWrapper(service.paperlessSettings(settingsToSend, journeyId).map {
                  case PreferencesExists | EmailUpdateOk => NoContent
                  case PreferencesCreated                => Created
                  case EmailNotExist                     => Conflict(toJson[ErrorResponse](ErrorPreferenceConflict))
                  case NoPreferenceExists                => NotFound(toJson[ErrorResponse](ErrorNotFound))
                  case _                                 => InternalServerError(toJson[ErrorResponse](PreferencesSettingsError))
                })
              }
            )
        }
      }
    }

  override def paperlessSettingsOptOut(journeyId: JourneyId): Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async(controllerComponents.parsers.json) { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          request.body
            .validate[PaperlessOptOut]
            .fold(
              errors => {
                logger.warn("Received error with service getPaperlessSettings: " + errors)
                Future successful BadRequest
              },
              settings => {
                val paperlessOptOutToSend =
                  if (!optInVersionsEnabled)
                    settings
                      .copy(generic = Some(settings.generic.getOrElse(TermsAccepted(Some(false))).copy(optInPage = None)))
                  else settings
                errorWrapper(service.paperlessSettingsOptOut(paperlessOptOutToSend).map {
                  case PreferencesExists       => NoContent
                  case PreferencesCreated      => Created
                  case PreferencesDoesNotExist => NotFound
                  case _                       => InternalServerError(toJson[ErrorResponse](PreferencesSettingsError))
                })
              }
            )
        }
      }
    }

  override def preferencesPendingEmail(journeyId: JourneyId): Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async(controllerComponents.parsers.json) { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          request.body
            .validate[ChangeEmail]
            .fold(
              errors => {
                logger.warn("Errors validating request body: " + errors)
                Future successful BadRequest
              },
              changeEmail =>
                errorWrapper(service.setPreferencesPendingEmail(changeEmail).map {
                  case EmailUpdateOk      => NoContent
                  case EmailNotExist      => Conflict
                  case NoPreferenceExists => NotFound
                  case _                  => InternalServerError(toJson[ErrorResponse](PreferencesSettingsError))
                })
            )
        }
      }
    }

}
