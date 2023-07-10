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

import com.google.inject.Singleton
import com.google.inject.Inject
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxApplePassController @Inject() (cc: ControllerComponents)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  val applePass = "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"

  def getApplePass(journeyId: JourneyId): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      Future successful Ok(toJson(RetrieveApplePass(applePass)))
    }

}
