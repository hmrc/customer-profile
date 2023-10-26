/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser.{ENROLMENT, WWW_AUTHENTICATE}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.domain.Nino

object AuthStub {
  private val authUrl: String = "/auth/authorise"

  private val authorisationRequestJsonNinoCheck: String =
    """{
      |   "authorise": [ {
      |     "enrolment" : "HMRC-NI",
      |     "identifiers" : [ {
      |        "key" : "NINO",
      |        "value" : "AA000006C"
      |     } ],
      |     "state" : "Activated"
      |   } ],
      |   "retrieve": ["nino", "confidenceLevel"]
      |}""".stripMargin

  private val authorisationRequestJson: String =
    """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin

  private val ninoRequestJson: String =
    """{ "authorise": [], "retrieve": ["nino"] }""".stripMargin

  def authRecordExists(
    nino:            Nino,
    confidenceLevel: ConfidenceLevel = L200
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(obj("confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino).toString)
        )
    )

  def authRecordExistsNinoCheck (
    nino:            Nino,
    confidenceLevel: ConfidenceLevel = L200
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJsonNinoCheck, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(obj("confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino).toString)
        )
    )

  def authFailure(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(WWW_AUTHENTICATE, """MDTP detail="BearerTokenExpired"""")
            .withHeader(ENROLMENT, "")
        )
    )

  def authFailureNinoCheck(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJsonNinoCheck, true, false))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(WWW_AUTHENTICATE, """MDTP detail="BearerTokenExpired"""")
            .withHeader(ENROLMENT, "")
        )
    )

  def authRecordExistsWithoutNino(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L200.level).toString))
    )

  def ninoFound(nino: Nino): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(ninoRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              obj(
                "nino" -> nino.nino
              ).toString
            )
        )
    )

  def accountsFailure(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(ninoRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withBody(s"""
                         |{
                         |  "error": "unauthorized"
                         |}
          """.stripMargin)
        )
    )

  def ninoNotFound(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(ninoRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )
}
