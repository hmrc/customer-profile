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

package uk.gov.hmrc.customerprofile.connector

import org.mockito.Mockito.when
import uk.gov.hmrc.customerprofile.domain.RetrieveGooglePass
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, TooManyRequestException}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class GooglePassConnectorSpec extends HttpClientV2Helper {

  val connector = app.injector.instanceOf[GooglePassConnector]
  val passId:    String = "c864139e-77b5-448f-b443-17c69060870d"
  val jwtString: String = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
  val fullName    = "Mr Joe Bloggs"
  val credentials = "dummyCredentials"

  "GooglePassConnector" when {
    "calling the createGooglePassWithCredentials" should {
      "return a UUID given the call is successful" in {
        val successResponse = HttpResponse(200, passId)

        when(requestBuilderExecute[HttpResponse])
          .thenReturn(Future.successful(successResponse))

        connector.createGooglePassWithCredentials(fullName, nino.formatted, credentials) onComplete {
          case Success(_) => successResponse.body
          case Failure(_) =>
        }

      }
      "return an exception if the call is unsuccessful" in {
        when(requestBuilderExecute[HttpResponse])
          .thenReturn(Future.failed(new BadRequestException("")))

        connector.createGooglePassWithCredentials(fullName, nino.formatted, credentials) onComplete {
          case Success(_) => fail()
          case Failure(_) =>
        }
      }
    }
    "calling the getGooglePass" should {
      "return a base 64 encoded string given the call is successful" in {
        when(requestBuilderExecute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(200, jwtString)))

        connector.getGooglePassUrl(passId) onComplete {
          case Success(_) => RetrieveGooglePass(jwtString)
          case Failure(_) =>
        }
      }
      "return 429 exception if the call is unsuccessful" in {
        when(requestBuilderExecute[HttpResponse])
          .thenReturn(Future.failed(new TooManyRequestException("")))

        connector.getGooglePassUrl(passId) onComplete {
          case Success(_) => fail()
          case Failure(_) =>
        }
      }
      "return an exception if the call is unsuccessful" in {

        when(requestBuilderExecute[HttpResponse])
          .thenReturn(Future.failed(new BadRequestException("")))

        connector.getGooglePassUrl(passId) onComplete {
          case Success(_) => fail()
          case Failure(_) =>
        }
      }
    }
  }

}
