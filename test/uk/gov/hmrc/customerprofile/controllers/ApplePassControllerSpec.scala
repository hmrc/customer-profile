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

import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.customerprofile.domain.{RetrieveApplePass}
import uk.gov.hmrc.customerprofile.services.ApplePassService
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.utils.AuthAndShutterMock
import uk.gov.hmrc.http.{NotFoundException, UpstreamErrorResponse}

import scala.concurrent.Future

class ApplePassControllerSpec extends AuthAndShutterMock {

  implicit val shutteringConnectorMock: ShutteringConnector =
    new ShutteringConnector(http = mockHttpClient, serviceUrl = s"http://baseUrl")

  val appleService: ApplePassService = mock[ApplePassService]
  val base64String = "TXIgSm9lIEJsb2dncw=="

  def mockAppleServiceDef(response: Future[RetrieveApplePass]) =
    when(appleService.getApplePass()(any(), any())).thenReturn(response)

  val applePassController: ApplePassController =
    new ApplePassController(mockAuthConnector, 200, appleService, components, shutteringConnectorMock)

  "getApplePass" should {

    "return an apple base 64 encoded string" in {
      val applePass: RetrieveApplePass = RetrieveApplePass(base64String)
      mockAuthAccessAndNotShuttered()
      mockAppleServiceDef(Future.successful(applePass))

      val response = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson(applePass)

    }

    "propagate 401" in {

      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val response = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(response) mustBe UNAUTHORIZED
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val response = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(response) mustBe UNAUTHORIZED

    }

    "return status code 406 when the headers are invalid" in {
      val result = applePassController.getApplePass(journeyId)(requestWithoutAcceptHeader)
      status(result) mustBe NOT_ACCEPTABLE
    }

    "return 500 for an unexpected error" in {

      mockAuthAccessAndNotShuttered()
      mockAppleServiceDef(Future.failed(new RuntimeException()))

      val result = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 500
    }

    "return 521 when shuttered" in {

      mockAuthAccessAndShuttered()

      val result =
        applePassController.getApplePass(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message")
        .as[String] mustBe "Customer-Profile is currently not available"
    }

    "return Unauthorized if failed to grant access" in {

      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe UNAUTHORIZED
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {

      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe FORBIDDEN
    }

    "return Unauthorised if Account with low CL" in {

      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe UNAUTHORIZED
    }

    "return 404 where the account does not exist" in {

      mockAuthAccessAndNotShuttered()
      mockAppleServiceDef(Future.failed(new NotFoundException("No resources found")))

      val result = applePassController.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 404
    }
  }

  "getAppleQRCode" should {

    val qrCode: Array[Byte] = "QRCodeData".getBytes()

    def mockAppleServiceQRCode(response: Future[Option[Array[Byte]]]) =
      when(appleService.getAppleQRCode()(any(), any())).thenReturn(response)

    "return an apple base 64 encoded string" in {
      mockAuthAccessAndNotShuttered()
      mockAppleServiceQRCode(Future.successful(Some(qrCode)))

      val response = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson(qrCode)

    }

    "propagate 401" in {

      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val response = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(response) mustBe UNAUTHORIZED
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val response = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(response) mustBe UNAUTHORIZED

    }

    "return status code 406 when the headers are invalid" in {
      val result = applePassController.getAppleQRCode(journeyId)(requestWithoutAcceptHeader)
      status(result) mustBe NOT_ACCEPTABLE
    }

    "return 500 for an unexpected error" in {

      mockAuthAccessAndNotShuttered()
      mockAppleServiceQRCode(Future.failed(new RuntimeException()))

      val result = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 500
    }

    "return 521 when shuttered" in {

      mockAuthAccessAndShuttered()

      val result =
        applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message")
        .as[String] mustBe "Customer-Profile is currently not available"
    }

    "return Unauthorized if failed to grant access" in {

      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(result) mustBe UNAUTHORIZED
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {

      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(result) mustBe FORBIDDEN
    }

    "return Unauthorised if Account with low CL" in {

      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(result) mustBe UNAUTHORIZED
    }

    "return 404 where the account does not exist" in {

      mockAuthAccessAndNotShuttered()
      mockAppleServiceQRCode(Future.failed(new NotFoundException("No resources found")))

      val result = applePassController.getAppleQRCode(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 404
    }

  }
}
