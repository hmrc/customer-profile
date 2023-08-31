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

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.Helpers.{contentAsJson, status, stubControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.customerprofile.auth.AccountAccessControl
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.domain.{RetrieveApplePass, Shuttering}
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.mocks.ShutteringMock
import uk.gov.hmrc.customerprofile.services.ApplePassService
import uk.gov.hmrc.domain.Nino
import eu.timepit.refined.auto._
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse, UpstreamErrorResponse}
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.auth.core.SessionRecordNotFound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ApplePassControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockFactory
    with ShutteringMock {

  val service:       ApplePassService     = mock[ApplePassService]
  val accessControl: AccountAccessControl = mock[AccountAccessControl]
  val base64String = "TXIgSm9lIEJsb2dncw=="

  implicit val shutteringConnectorMock: ShutteringConnector =
    mock[ShutteringConnector]

  val shuttered: Shuttering =
    Shuttering(
      shuttered = true,
      Some("Shuttered"),
      Some("Get Apple Pass is currently not available")
    )
  val notShuttered: Shuttering = Shuttering.shutteringDisabled

  val controller: ApplePassController =
    new ApplePassController(service, accessControl, stubControllerComponents(), shutteringConnectorMock)

  val nino:         Nino      = Nino("CS700100A")
  val journeyId:    JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val acceptheader: String    = "application/vnd.hmrc.1.0+json"

  val requestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Accept" -> acceptheader)
  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val requestWithoutAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Authorization" -> "Some Header")

  def authSuccess(maybeNino: Option[Nino] = None) =
    (accessControl
      .grantAccess(_: Option[Nino])(_: HeaderCarrier))
      .expects(maybeNino, *)
      .returns(Future.successful(()))

  def authError(
    e:         Exception,
    maybeNino: Option[Nino] = None
  ) =
    (accessControl
      .grantAccess(_: Option[Nino])(_: HeaderCarrier))
      .expects(maybeNino, *)
      .returns(Future failed e)

  "getApplePass" should {

    def mockGetApplePass(result: Future[RetrieveApplePass]) =
      (service
        .getApplePass()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(result)

    "return an apple base 64 encoded string" in {
      val applePass = RetrieveApplePass(base64String)
      authSuccess(None)
      mockGetApplePass(Future successful applePass)
      mockShutteringResponse(notShuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(applePass)
    }

    "propagate 401" in {
      authError(new SessionRecordNotFound)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getApplePass(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      authSuccess()
      mockGetApplePass(Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
    "return 521 when shuttered" in {
      authSuccess()
      mockShutteringResponse(shuttered)

      val result =
        controller.getApplePass(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Get Apple Pass is currently not available"
    }

    "return Unauthorized if failed to grant access" in {
      authError(UpstreamErrorResponse("ERROR", 403, 403))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {
      authError(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return Forbidden if Account with low CL" in {
      authError(new AccountWithLowCL("ERROR"))

      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }
    "return 404 where the account does not exist" in {
      authSuccess()
      mockShutteringResponse(notShuttered)
      mockGetApplePass(Future failed new NotFoundException("No resources found"))
      val result = controller.getApplePass(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 404
    }
  }
}
