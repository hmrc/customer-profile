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

import uk.gov.hmrc.http.{BadRequestException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class CitizenDetailsConnectorSpec extends HttpClientV2Helper {

    val connector = app.injector.instanceOf[CitizenDetailsConnector]

  "citizenDetailsConnector" should {
    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.failed(new BadRequestException("bad request")))
      connector.personDetails(nino) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "throw Upstream5xxResponse when a 500 response is returned" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", 500, 500)))
      connector.personDetails(nino) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }
  }
}
