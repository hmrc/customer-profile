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

package uk.gov.hmrc.customerprofile

import java.io.InputStream

import org.scalatest.concurrent.Eventually
import play.api.libs.json.Json.{parse, toJson}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.customerprofile.domain.NativeOS.iOS
import uk.gov.hmrc.customerprofile.domain.{DeviceVersion, Paperless, TermsAccepted}
import uk.gov.hmrc.customerprofile.stubs.AuthStub._
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.{designatoryDetailsForNinoAre, designatoryDetailsWillReturnErrorResponse, npsDataIsLockedDueToMciFlag}
import uk.gov.hmrc.customerprofile.stubs.EntityResolverStub._
import uk.gov.hmrc.customerprofile.stubs.PreferencesStub.{conflictPendingEmailUpdate, errorPendingEmailUpdate, successfulPendingEmailUpdate}
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.io.Source.fromInputStream

class CustomerProfileISpec extends BaseISpec with Eventually {
  val nino = Nino("AA000006C")
  val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def getRequestWithAcceptHeader(url: String): Future[WSResponse] = wsUrl(url).withHeaders(acceptJsonHeader).get()

  def postRequestWithAcceptHeader(url: String, form: JsValue): Future[WSResponse] =
    await(wsUrl(url).withHeaders(acceptJsonHeader).post(form))

  def postRequestWithAcceptHeader(url: String): Future[WSResponse] =
    await(wsUrl(url).withHeaders(acceptJsonHeader).post(""))

  "GET /profile/accounts" should {
    val url: String = "/profile/accounts"

    "return account details" in {
      accountsFound(nino)

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 200
      ( response.json \ "nino" ).as[String] shouldBe nino.nino
    }

    "return 200 if no nino on account" in {
      accountsFoundWithoutNino()

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 200
      ( response.json \ "nino" ).asOpt[String] shouldBe None
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).get().status shouldBe 406
    }

    "propagate 401" in {
      accountsFailure()
      getRequestWithAcceptHeader(url).status shouldBe 401
    }
  }

  "GET /profile/preferences" should {
    val url = "/profile/preferences"

    "return preferences" in {
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedIn()

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 200
      ( response.json \ "digital" ).as[Boolean] shouldBe true
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).get().status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      getRequestWithAcceptHeader(url).status shouldBe 401
    }
  }

  "POST /profile/native-app/version-check" should {
    val url = "/profile/native-app/version-check"
    val version: JsValue = toJson(DeviceVersion(iOS, "0.1"))

    "return a version check response with no auth required" in {
      val response = postRequestWithAcceptHeader(url, version)

      response.status shouldBe 200
      ( response.json \ "upgrade" ).as[Boolean] shouldBe true
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).post(version).status shouldBe 406
    }
  }

  "GET /profile/personal-details/:nino" should {
    val url = s"/profile/personal-details/${nino.value}"
    "return personal details for the given NINO from citizen-details" in {
      designatoryDetailsForNinoAre(nino, resourceAsString("AA000006C-citizen-details.json").get)
      authRecordExists(nino)

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 200
      response.json shouldBe getResourceAsJsValue("expected-AA000006C-personal-details.json")
    }

    "return a 423 response status code when the NINO is locked due to Manual Correspondence Indicator flag being set in NPS" in {
      npsDataIsLockedDueToMciFlag(nino)
      authRecordExists(nino)

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 423
      response.json shouldBe parse("""{"code":"MANUAL_CORRESPONDENCE_IND","message":"Data cannot be disclosed to the user because MCI flag is set in NPS"}""")
    }

    "return 500 response status code when citizen-details returns 500 response status code." in {
      designatoryDetailsWillReturnErrorResponse(nino, 500)
      authRecordExists(nino)

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 500
      response.json shouldBe parse("""{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}""")
    }

    "return 404 response status code when citizen-details returns 404 response status code." in {
      designatoryDetailsWillReturnErrorResponse(nino, 404)
      authRecordExists(nino)

      val response = getRequestWithAcceptHeader(url)

      response.status shouldBe 404
      response.json shouldBe parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).get().status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      getRequestWithAcceptHeader(url).status shouldBe 401
    }
  }

  "POST /profile/paperless-settings/opt-in" should {
    val url = "/profile/preferences/paperless-settings/opt-in"
    val entityId = "1098561938451038465138465"
    val paperless = toJson(Paperless(generic = TermsAccepted(true), email = EmailAddress("new-email@new-email.new.email")))

    "return a 200 response when successfully opting into paperless settings" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesNoPaperlessSet()
      authRecordExists(nino)
      successPaperlessSettingsChange()
      accountsFound(nino)

      postRequestWithAcceptHeader(url, paperless).status shouldBe 200
    }

    "return a 200 response when a pending email preference is successfully updated" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesWithPaperlessOptedIn()
      authRecordExists(nino)
      successfulPendingEmailUpdate(entityId)
      accountsFound(nino)

      postRequestWithAcceptHeader(url, paperless).status shouldBe 200
    }

    "return a Conflict response when preferences has no existing verified or pending email" in {
      val expectedResponse = parse("""{"code":"CONFLICT","message":"No existing verified or pending data"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondPreferencesWithBouncedEmail()
      conflictPendingEmailUpdate(entityId)
      accountsFound(nino)

      val response = postRequestWithAcceptHeader(url, paperless)
      response.status shouldBe 409
      response.json shouldBe expectedResponse
    }

    "return a Not Found response when unable to find a preference to update for an entity" in {
      val expectedResponse = parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondNoPreferences()

      val response = postRequestWithAcceptHeader(url, paperless)
      response.status shouldBe 404
      response.json shouldBe expectedResponse
    }

    "return a Internal Server Error response when unable update pending email preference for an entity" in {
      val expectedResponse = parse("""{"code":"PREFERENCE_SETTINGS_ERROR","message":"Failed to set preferences"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedIn()
      errorPendingEmailUpdate(entityId)
      accountsFound(nino)

      val response = postRequestWithAcceptHeader(url, paperless)
      response.status shouldBe 500
      response.json shouldBe expectedResponse
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).post(paperless).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      postRequestWithAcceptHeader(url, paperless).status shouldBe 401
    }
  }

  "POST /profile/paperless-settings/opt-out" should {
    val url = "/profile/preferences/paperless-settings/opt-out"

    "return a 200 response when successful" in {
      authRecordExists(nino)
      successPaperlessSettingsChange()

      postRequestWithAcceptHeader(url).status shouldBe 200
    }

    "return 406 if no request header is supplied" in {
      wsUrl(url).post("").status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      postRequestWithAcceptHeader(url).status shouldBe 401
    }
  }

  private def resourceAsString(resourcePath: String): Option[String] =
    withResourceStream(resourcePath) { is =>
      fromInputStream(is).mkString
    }

  private def resourceAsJsValue(resourcePath: String): Option[JsValue] =
    withResourceStream(resourcePath) { is =>
      Json.parse(is)
    }

  private def getResourceAsJsValue(resourcePath: String): JsValue =
    resourceAsJsValue(resourcePath).getOrElse(throw new RuntimeException(s"Could not find resource $resourcePath"))

  private def withResourceStream[A](resourcePath: String)(f: (InputStream => A)): Option[A] =
    Option(getClass.getResourceAsStream(resourcePath)) map { is =>
      try {
        f(is)
      } finally {
        is.close()
      }
    }

}
