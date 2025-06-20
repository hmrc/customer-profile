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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status, stubControllerComponents}
import uk.gov.hmrc.customerprofile.connector.CitizenDetailsConnector
import uk.gov.hmrc.customerprofile.domain.MobilePin
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, MongoService}
import uk.gov.hmrc.customerprofile.utils.{AuthAndShutterMock, BaseSpec, HashSaltUtils}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class ValidateControllerSpec extends BaseSpec with AuthAndShutterMock {

  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockMongoService = mock[MongoService]
  val mockCustomerProfileService = mock[CustomerProfileService]
  val controllerComponents: ControllerComponents = stubControllerComponents()
  val acceptHeaderMap: (String, String) = "Accept" -> acceptHeader

  val controller = new ValidateController(
    mockAuthConnector,
    mockCitizenDetailsConnector,
    mockMongoService,
    mockCustomerProfileService,
    confLevel           = 200,
    storedPinCount      = 3,
    dobErrorKey         = "dob_error",
    previousPinErrorKey = "prev_pin_error",
    controllerComponents
  )

  val deviceId = "6D92078A-8246-4BA4-AE5B-76104861E7DC"
  val deviceId1 = "7D92078A-8246-4BA4-AE5B-76104861E7DC"
  val dob1 = LocalDate.of(1980, 7, 24)
  val fakeRequest = FakeRequest("GET", "/").withHeaders(acceptHeaderMap)
  val uuid = UUID.randomUUID().toString

  val mobilePin = MobilePin(uuid, hashNino, List(hash11, hash2))

  "ValidateControllerSpec" should {

    "return 200" when {

      "valid pin is entered while pin creation" in {
        mockAuthorisationGrantAccess(grantAccessWithCL200)
        when(mockCustomerProfileService.getNino()(any(), any()))
          .thenReturn(Future.successful(Some(nino)))
        when(mockCitizenDetailsConnector.personDetailsForPin(any())(any(), any()))
          .thenReturn(Future.successful(Some(person4)))
        when(mockMongoService.findByDeviceIdAndNinoHash(any(), any())(any()))
          .thenReturn(Future.successful(None))
        val result = controller.validatePin("300684", deviceId, journeyId)(fakeRequest)
        val jsonBody = contentAsJson(result)
        status(result) mustBe 200
        (jsonBody \ "key").as[String] mustBe "valid_pin"
      }

      "valid pin is entered if the deviceId and nino combination exists in DB" in {
        mockAuthorisationGrantAccess(grantAccessWithCL200)
        when(mockCustomerProfileService.getNino()(any(), any()))
          .thenReturn(Future.successful(Some(nino)))
        when(mockCitizenDetailsConnector.personDetailsForPin(any())(any(), any()))
          .thenReturn(Future.successful(Some(person4)))
        when(mockMongoService.findByDeviceIdAndNinoHash(any(), any())(any()))
          .thenReturn(Future.successful(Some(mobilePin)))

        val result = controller.validatePin("300685", deviceId, journeyId)(fakeRequest)
        val jsonBody = contentAsJson(result)
        status(result) mustBe 200
        (jsonBody \ "key").as[String] mustBe "valid_pin"

      }

      "a pin matching dob is entered while pin creation" in {

        mockAuthorisationGrantAccess(grantAccessWithCL200)
        when(mockCustomerProfileService.getNino()(any(), any()))
          .thenReturn(Future.successful(Some(nino)))
        when(mockCitizenDetailsConnector.personDetailsForPin(any())(any(), any()))
          .thenReturn(Future.successful(Some(person4)))
        val result = controller.validatePin("300686", deviceId, journeyId)(fakeRequest)
        val jsonBody = contentAsJson(result)
        status(result) mustBe 200
        (jsonBody \ "key").as[String] mustBe "dob_error"

      }

      "a pin matching previous pin  is entered while pin update" in {

        mockAuthorisationGrantAccess(grantAccessWithCL200)
        when(mockCustomerProfileService.getNino()(any(), any()))
          .thenReturn(Future.successful(Some(nino)))
        when(mockCitizenDetailsConnector.personDetailsForPin(any())(any(), any()))
          .thenReturn(Future.successful(Some(person4)))
        when(mockMongoService.findByDeviceIdAndNinoHash(any(), any())(any()))
          .thenReturn(Future.successful(Some(mobilePin)))

        val result = controller.validatePin("240712", deviceId, journeyId)(fakeRequest)
        val jsonBody = contentAsJson(result)
        status(result) mustBe 200
        (jsonBody \ "key").as[String] mustBe "prev_pin_error"

      }

    }
  }

}
