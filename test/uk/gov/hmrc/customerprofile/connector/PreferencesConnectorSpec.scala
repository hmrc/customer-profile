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
import play.api.http.Status.{CONFLICT, IM_A_TEAPOT, NOT_FOUND}
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http.{HttpResponse}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class PreferencesConnectorSpec extends HttpClientV2Helper {

  val baseUrl:             String        = "http://baseUrl"
  val externalServiceName: String        = "externalServiceName"
  val entityId:            String        = "entityId"
  val changeEmailRequest:  ChangeEmail   = ChangeEmail(email = "some-new-email@newEmail.new.email")

  val connector: PreferencesConnector =
    new PreferencesConnector(mockHttpClient, baseUrl, externalServiceName, config, environment)

  "updatePendingEmail()" should {

    "return status EmailUpdateOk when the service returns an OK status" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(200, "")))

      connector.updatePendingEmail(changeEmailRequest, entityId) onComplete {
        case Success(_) => EmailUpdateOk
        case Failure(_) =>
      }

    }

    "handle 404 NOT_FOUND response" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

      connector.updatePendingEmail(changeEmailRequest, entityId) onComplete {
        case Success(_) => NoPreferenceExists
        case Failure(_) =>
      }

    }

    "handle 409 CONFLICT response" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(CONFLICT, "")))

      connector.updatePendingEmail(changeEmailRequest, entityId) onComplete {
        case Success(_) => EmailNotExist
        case Failure(_) =>
      }
    }

    "handles exceptions" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(IM_A_TEAPOT, "")))

      connector.updatePendingEmail(changeEmailRequest, entityId) onComplete {
        case Success(_) => EmailUpdateFailed
        case Failure(_) =>
      }
    }
  }
}
