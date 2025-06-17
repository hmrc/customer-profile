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
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.auth.*
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.domain.types.JourneyId
import uk.gov.hmrc.customerprofile.services.ApplePassService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import javax.inject.Named
import scala.concurrent.ExecutionContext

@Singleton
class ApplePassController @Inject() (
  override val authConnector: AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  service: ApplePassService,
  controllerComponents: ControllerComponents,
  shutteringConnector: ShutteringConnector
)(implicit val executionContext: ExecutionContext)
    extends BackendController(controllerComponents)
    with AccessControl
    with ErrorHandling
    with ControllerChecks {
  outer =>
  override val logger: Logger = Logger(this.getClass)
  def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
  val app = "Apple Pass Controller"

  def getApplePass(journeyId: JourneyId): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async { implicit request =>
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
