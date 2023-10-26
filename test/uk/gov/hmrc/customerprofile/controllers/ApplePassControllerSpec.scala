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

import play.api.test.Helpers.{contentAsJson, status, stubControllerComponents}
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass
import uk.gov.hmrc.customerprofile.services.ApplePassService
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class ApplePassControllerSpec extends BaseSpec {

  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val service: ApplePassService = mock[ApplePassService]
  val base64String = "TXIgSm9lIEJsb2dncw=="

  val controller: ApplePassController =
    new ApplePassController(mockAuthConnector, 200, service, stubControllerComponents(), shutteringConnectorMock)

  "getApplePass" should {

    def mockGetApplePass(result: Future[RetrieveApplePass]) =
      (service
        .getApplePass()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(result)

    "return an apple base 64 encoded string" in {
      val applePass = RetrieveApplePass(base64String)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetApplePass(Future successful applePass)
      mockShutteringResponse(notShuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(applePass)
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getApplePass(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetApplePass(Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }

    "return Unauthorized if failed to grant access" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {
      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return Unauthorised if Account with low CL" in {
      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }
    "return 404 where the account does not exist" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      mockGetApplePass(Future failed new NotFoundException("No resources found"))
      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 404
    }
  }
}
