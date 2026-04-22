/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.domain.types.JourneyId
import uk.gov.hmrc.customerprofile.auth.AccessControl
import uk.gov.hmrc.customerprofile.response.ValidateResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxValidateController @Inject() (
  override val authConnector: AuthConnector,
  @Named("controllers.confidenceLevel") val confLevel: Int,
  @Named("service.maxStoredPins") val storedPinCount: Int,
  @Named("dobErrorKey") val dobErrorKey: String,
  @Named("previousPinErrorKey") val previousPinErrorKey: String,
  controllerComponents: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(controllerComponents)
    with AccessControl
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
  override val logger: Logger = Logger(this.getClass)

  def validatePin(
    enteredPin: String,
    deviceId: String,
    journeyId: JourneyId
  ): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>

    enteredPin match {
      case "310199" | "319901" | "013199" | "019931" | "990131" | "993101" | "319901" | "199901" | "199931" | "011999" | "311999" | "199931" =>
        Future.successful(Ok(Json.toJson(ValidateResponse(Some("dob_error"), "PIN should not include your date of birth"))))
      case _ => Future.successful(Ok(Json.toJson(ValidateResponse(Some("valid_pin"), "Pin is valid"))))
    }

  }

}
