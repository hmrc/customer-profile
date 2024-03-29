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

import play.api.http.Status.{CONFLICT, IM_A_TEAPOT, NOT_FOUND}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customerprofile.config.WSHttpImpl
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class PreferencesConnectorSpec extends BaseSpec {

  val http:                WSHttpImpl    = mock[WSHttpImpl]
  val config:              Configuration = mock[Configuration]
  val environment:         Environment   = mock[Environment]
  val baseUrl:             String        = "baseUrl"
  val externalServiceName: String        = "externalServiceName"
  val entityId:            String        = "entityId"
  val changeEmailRequest:  ChangeEmail   = ChangeEmail(email = "some-new-email@newEmail.new.email")

  val connector: PreferencesConnector =
    new PreferencesConnector(http, baseUrl, externalServiceName, config, environment)

  "updatePendingEmail()" should {
    def mockPUT(response: Future[HttpResponse]) =
      (http
        .PUT(_: String, _: JsValue, _: Seq[(String, String)])(_: Writes[JsValue],
                                                              _: HttpReads[HttpResponse],
                                                              _: HeaderCarrier,
                                                              _: ExecutionContext))
        .expects(s"$baseUrl/preferences/$entityId/pending-email", Json.toJson(changeEmailRequest), *, *, *, *, *)
        .returns(response)

    "return status EmailUpdateOk when the service returns an OK status" in {
      mockPUT(Future successful HttpResponse(200, ""))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailUpdateOk
    }

    "handle 404 NOT_FOUND response" in {
      mockPUT(Future successful HttpResponse(NOT_FOUND, ""))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe NoPreferenceExists
    }

    "handle 409 CONFLICT response" in {
      mockPUT(Future successful HttpResponse(CONFLICT, ""))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailNotExist
    }

    "handles exceptions" in {
      mockPUT(Future successful HttpResponse(IM_A_TEAPOT, ""))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailUpdateFailed
    }
  }
}
