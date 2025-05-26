/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.auth.AccessControl
import uk.gov.hmrc.customerprofile.connector.CitizenDetailsConnector
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.response.ValidateResponse
import uk.gov.hmrc.customerprofile.services.MongoService
import uk.gov.hmrc.customerprofile.utils.DOBUtils
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import java.time.LocalDate
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateController @Inject() (
  override val authConnector:                            AuthConnector,
  citizenDetailsConnector:                               CitizenDetailsConnector,
  mongoService:                                          MongoService,
  @Named("controllers.confidenceLevel") val confLevel:   Int,
  @Named("service.maxStoredPins") val storedPinCount:    Int,
  @Named("dobErrorKey") val dobErrorKey:                 String,
  @Named("previousPinErrorKey") val previousPinErrorKey: String,
  controllerComponents:                                  ControllerComponents
)(implicit val executionContext:                         ExecutionContext)
    extends BackendController(controllerComponents)
    with ErrorHandling
    with AccessControl
    with HeaderValidator {

  override val app:    String                 = "mobile-pin-security"
  override val logger: Logger                 = Logger(this.getClass)
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  def validatePin(
    enteredPin: String,
    deviceId:   String,
    journeyId:  JourneyId,
    mode:       String
  ): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      withNinoFromAuth { nino =>
        errorWrapper {
          citizenDetailsConnector.personDetailsForPin(Nino(nino)).flatMap { personDetails =>
            val dob: Option[LocalDate] = personDetails.flatMap(_.person.personDateOfBirth)

            DOBUtils.matchesDOBPatterns(dob, enteredPin) match {
              case true =>
                logger.info("Unauthorized! Pin shouldn't match DOB!")
                Future.successful(
                  Unauthorized(
                    Json.toJson(
                      ValidateResponse(Some(dobErrorKey), "PIN should not include your date of birth")
                    )
                  )
                )
              case false =>
                if (mode == "updatePin") {
                  mongoService.getLastThreePin(deviceId).flatMap { hashedPins =>
                    if (hashedPins
                          .takeRight(storedPinCount)
                          .exists(storedHash => BCrypt.checkpw(enteredPin, storedHash))) {
                      logger.info("Entered Pin can't be same as last three pins!")
                      Future.successful(
                        Unauthorized(
                          Json.toJson(
                            ValidateResponse(Some(previousPinErrorKey), "Do not re-use an old PIN")
                          )
                        )
                      )
                    } else {
                      logger.info("Successful! Entered Pin is valid")
                      Future.successful(Ok(Json.toJson(ValidateResponse(None, "Pin is valid"))))
                    }
                  }
                } else {
                  logger.info("Successful! Entered Pin is valid")
                  Future.successful(Ok(Json.toJson(ValidateResponse(None, "Pin is valid"))))
                }

            }
          }

        }
      }

    }
}
