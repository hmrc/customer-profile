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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.*
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.domain.MobilePinValidatedRequest
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, MobilePinService, MongoService}
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MobilePinControllerSpec extends BaseSpec with MockitoSugar with Results {

  val mockPinService: MobilePinService = mock[MobilePinService]
  val auditConnector: AuditConnector = mock[AuditConnector]
  val controllerComponents: ControllerComponents = stubControllerComponents()
  val acceptHeaderMap: (String, String) = "Accept" -> acceptHeader
  val uuid = UUID.randomUUID().toString
  val mockCustomerProfileService: CustomerProfileService = mock[CustomerProfileService]

  val jsonRequest = Json.obj(
    "pin"      -> "828936",
    "deviceId" -> uuid
  )

  val fakeRequest: FakeRequest[jsonRequest.type] =
    FakeRequest("PUT", "/").withHeaders(acceptHeaderMap).withBody(jsonRequest)

  val controller = new MobilePinController(
    mockAuthConnector,
    confLevel = 200,
    controllerComponents,
    auditConnector,
    pinService = mockPinService,
    mockCustomerProfileService
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
      when(mockCustomerProfileService.getNino()(any(), any()))
        .thenReturn(Future.successful(Some(nino)))
      mockUpsertPin(Future.successful(()))
      val result = controller.upsert(fakeRequest)

      status(result) mustBe CREATED
    }

    "return BadRequest (400) for invalid request body" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      when(mockCustomerProfileService.getNino()(any(), any()))
        .thenReturn(Future.successful(Some(nino)))
      val invalidJson = Json.obj("invalidField" -> "oops")
      mockUpsertPin(Future.successful(()))
      val request = FakeRequest(PUT, "/upsert")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .withBody(invalidJson)
      val result = controller.upsert(request)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] mustBe "Invalid request format"
    }
  }
}
