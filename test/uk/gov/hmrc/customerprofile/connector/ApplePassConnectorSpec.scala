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

package uk.gov.hmrc.customerprofile.connector

import play.api.libs.json.Writes
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpReads, HttpResponse, TooManyRequestException}

import scala.concurrent.{ExecutionContext, Future}

class ApplePassConnectorSpec extends BaseSpec {

  val http:         HttpClient         = mock[HttpClient]
  val connector:    ApplePassConnector = new ApplePassConnector(http, "someUrl")
  val passId:       String             = "c864139e-77b5-448f-b443-17c69060870d"
  val base64String: String             = "TXIgSm9lIEJsb2dncw=="
  val fullName = "Mr Joe Bloggs"

  def performSuccessfulPOST[I, O](response: Future[O])(implicit http: HttpClient) =
    (
      http
        .POST[I, O](_: String, _: I, _: Seq[(String, String)])(
          _: Writes[I],
          _: HttpReads[O],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *, *)
      .returns(response)

  def performUnsuccessfulPOST[I, O](response: Exception)(implicit http: HttpClient) =
    (
      http
        .POST[I, O](_: String, _: I, _: Seq[(String, String)])(
          _: Writes[I],
          _: HttpReads[O],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *, *)
      .returns(Future failed response)

  def performSuccessfulGET[O](response: Future[O])(implicit http: HttpClient) =
    (
      http
        .GET[O](_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[O],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *)
      .returns(response)

  def performUnsuccessfulGET(response: Exception)(implicit http: HttpClient) =
    (
      http
        .GET[HttpResponse](_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[HttpResponse],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *)
      .returns(Future failed response)

  "ApplePassConnector" when {
    "calling the createApplePass" should {
      "return a UUID given the call is successful" in {
        val successResponse = HttpResponse(200, passId)
        performSuccessfulPOST(Future.successful(successResponse))(http)
        val result = await(connector.createApplePass(nino.formatted, "Mr Joe Bloggs"))
        result shouldBe successResponse.body
      }
      "return an exception if the call is unsuccessful" in {
        performUnsuccessfulPOST(new BadRequestException(""))(http)
        intercept[BadRequestException] {
          await(connector.createApplePass(nino.formatted, "Mr Joe Bloggs"))
        }
      }
    }
    "calling the getPass" should {
      "return a base 64 encoded string given the call is successful" in {
        performSuccessfulGET(Future successful HttpResponse(200, base64String))(http)
        val result = await(connector.getApplePass(passId))
        result shouldBe RetrieveApplePass(base64String)
      }
      "return 429 exception if the call is unsuccessful" in {
        performUnsuccessfulGET(new TooManyRequestException(""))(http)
        intercept[TooManyRequestException] {
          await(connector.getApplePass(passId))
        }
      }
      "return an exception if the call is unsuccessful" in {
        performUnsuccessfulGET(new BadRequestException(""))(http)
        intercept[BadRequestException] {
          await(connector.getApplePass(passId))
        }
      }
    }
  }
}
