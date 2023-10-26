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

import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customerprofile.auth.AccessControl
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino

trait CustomerProfileController extends AccessControl {

  override val logger: Logger = Logger(this.getClass)

  def controllerComponents: ControllerComponents

  def getPersonalDetails(
    nino:      Nino,
    journeyId: JourneyId
  ): Action[AnyContent]

  def getPreferences(journeyId: JourneyId): Action[AnyContent]

  def paperlessSettingsOptOut(journeyId: JourneyId): Action[JsValue]

  def preferencesPendingEmail(journeyId: JourneyId): Action[JsValue]

  def paperlessSettingsOptIn(journeyId: JourneyId): Action[JsValue]

}
