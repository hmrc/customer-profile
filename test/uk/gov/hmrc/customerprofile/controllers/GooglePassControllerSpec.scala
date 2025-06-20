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
import org.mockito.Mockito.when
import uk.gov.hmrc.customerprofile.services.GooglePassService
import uk.gov.hmrc.customerprofile.domain.*
import play.api.test.Helpers.*
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.{NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.utils.AuthAndShutterMock

import scala.concurrent.Future

class GooglePassControllerSpec extends AuthAndShutterMock {

  implicit val shutteringConnectorMock: ShutteringConnector =
    new ShutteringConnector(http = mockHttpClient, serviceUrl = s"http://baseUrl")
  val googleService: GooglePassService = mock[GooglePassService]
  val jwtToken: String = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"

  val googlePassController: GooglePassController =
    new GooglePassController(mockAuthConnector, 200, googleService, components, shutteringConnectorMock)

  def mockGoogleServiceDef(response: Future[RetrieveGooglePass]) =
    when(googleService.getGooglePass()(any(), any())).thenReturn(response)

  "getGooglePass" should {

    "return a google pass url with a jwt token" in {
      val googlePass = RetrieveGooglePass(jwtToken)

      mockAuthAccessAndNotShuttered()
      mockGoogleServiceDef(Future.successful(googlePass))

      val result = googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result) mustBe toJson(googlePass)
    }

    "propagate 401" in {

      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }
    "return status code 406 when the headers are invalid" in {
      val result = googlePassController.getGooglePass(journeyId)(requestWithoutAcceptHeader)
      status(result) mustBe 406
    }

    "return 500 for an unexpected error" in {

      mockAuthAccessAndNotShuttered()
      mockGoogleServiceDef(Future.failed(new RuntimeException()))

      val result =
        googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 500
    }
    "return 521 when shuttered" in {

      mockAuthAccessAndShuttered()

      val result =
        googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message")
        .as[String] mustBe "Customer-Profile is currently not available"
    }

    "return Unauthorized if failed to grant access" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {
      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 403
    }

    "return Unauthorised if Account with low CL" in {

      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }
    "return 404 where the account does not exist" in {

      mockAuthAccessAndNotShuttered()
      mockGoogleServiceDef(Future.failed(new NotFoundException("No resources found")))

      val result = googlePassController.getGooglePass(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 404
    }
  }
}
