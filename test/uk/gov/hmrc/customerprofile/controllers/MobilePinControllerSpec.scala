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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.domain.MobilePinValidatedRequest
import uk.gov.hmrc.customerprofile.services.{MobilePinService, MongoService}
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MobilePinControllerSpec extends BaseSpec with MockitoSugar with Results {

  val mockPinService:       MobilePinService     = mock[MobilePinService]
  val auditConnector:       AuditConnector       = mock[AuditConnector]
  val controllerComponents: ControllerComponents = stubControllerComponents()
  val acceptHeader:         (String, String)     = "Accept" -> "application/vnd.hmrc.1.0+json"
  val uuid = UUID.randomUUID().toString

  val jsonRequest = Json.obj(
    "pin"      -> "828936",
    "deviceId" -> uuid,
    "nino"     -> nino.nino
  )

  val fakeRequest: FakeRequest[jsonRequest.type] =
    FakeRequest("PUT", "/").withHeaders(acceptHeader).withBody(jsonRequest)

  val controller = new MobilePinController(
    mockAuthConnector,
    confLevel = 200,
    controllerComponents,
    auditConnector,
    pinService = mockPinService
  )

  def mockAuthorisationGrantAccess(response: GrantAccess)(implicit authConnector: AuthConnector) =
    when(
      authConnector
        .authorise[GrantAccess](any(), any())(any(), any())
    ).thenReturn(Future.successful(response))

  private def mockUpsertPin(response: Future[Unit]) =
    when(mockPinService.upsertPin(any[MobilePinValidatedRequest], any())(any[ExecutionContext]))
      .thenReturn(response)

  "MobilePinController#upsert" should {

    "return Created (201) for valid request" in {

      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockUpsertPin(Future.successful(()))
      val result = controller.upsert(nino)(fakeRequest)

      status(result) mustBe CREATED
    }

    "return BadRequest (400) for invalid request body" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      val invalidJson = Json.obj("invalidField" -> "oops")
      mockUpsertPin(Future.successful(()))
      val request = FakeRequest(PUT, "/mobile-pin/upsert")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .withBody(invalidJson)
      val result = controller.upsert(nino)(request)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] mustBe "Invalid request format"
    }

    "Return Forbidden (403) when the nino is wrong" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockUpsertPin(Future.successful(()))
      val result = controller.upsert(Nino("AA123456D"))(fakeRequest)

      status(result) mustBe FORBIDDEN

    }
  }
}
