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

import uk.gov.hmrc.customerprofile.services.GooglePassService
import uk.gov.hmrc.customerprofile.domain._
import play.api.test.Helpers._
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class GooglePassControllerSpec extends BaseSpec {

  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val service:  GooglePassService = mock[GooglePassService]
  val jwtToken: String            = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"

  val controller: GooglePassController =
    new GooglePassController(mockAuthConnector, 200, service, stubControllerComponents(), shutteringConnectorMock)

  "getGooglePass" should {

    def mockGetGooglePass(result: Future[RetrieveGooglePass]) =
      (service
        .getGooglePass()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(result)

    "return a google pass url with a jwt token" in {
      val googlePass = RetrieveGooglePass(jwtToken)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetGooglePass(Future successful googlePass)
      mockShutteringResponse(notShuttered)

      val result = controller.getGooglePass(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(googlePass)
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }
    "return status code 406 when the headers are invalid" in {
      val result = controller.getGooglePass(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetGooglePass(Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result =
        controller.getGooglePass(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }

    "return Unauthorized if failed to grant access" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {
      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return Unauthorised if Account with low CL" in {
      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }
    "return 404 where the account does not exist" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      mockGetGooglePass(Future failed new NotFoundException("No resources found"))
      val result = controller.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 404
    }
  }
}
