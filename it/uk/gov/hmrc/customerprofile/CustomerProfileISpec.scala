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

import org.scalatest.concurrent.Eventually
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.{parse, toJson}
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.{ChangeEmail, OptInPage, PageType, Paperless, PaperlessOptOut, Shuttering, TermsAccepted, Version}
import uk.gov.hmrc.customerprofile.stubs.AuthStub._
import uk.gov.hmrc.customerprofile.stubs.ShutteringStub._
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.{designatoryDetailsForNinoAre, designatoryDetailsWillReturnErrorResponse, npsDataIsLockedDueToMciFlag}
import uk.gov.hmrc.customerprofile.stubs.EntityResolverStub._
import uk.gov.hmrc.customerprofile.stubs.PreferencesStub.{conflictPendingEmailUpdate, errorPendingEmailUpdate, successfulPendingEmailUpdate}
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.emailaddress.EmailAddress

import java.time.LocalDate

trait CustomerProfileTests extends BaseISpec with Eventually {

  "GET /profile/preferences" should {
    val url = s"/profile/preferences?journeyId=$journeyId"

    "return preferences" in {
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedIn()
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                shouldBe 200
      (response.json \ "digital").as[Boolean]        shouldBe true
      (response.json \ "status" \ "name").as[String] shouldBe "verified"
    }

    "return preferences if opted out" in {
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedOut()
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                shouldBe 200
      (response.json \ "digital").as[Boolean]        shouldBe false
      (response.json \ "email").isEmpty              shouldBe true
      (response.json \ "status" \ "name").as[String] shouldBe "Paper"
      (response.json \ "linkSent").isEmpty           shouldBe true
      (response.json \ "emailAddress").isEmpty       shouldBe true

    }

    "return preferences if email bounced" in {
      authRecordExists(nino)
      respondPreferencesWithBouncedEmail()
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                shouldBe 200
      (response.json \ "digital").as[Boolean]        shouldBe true
      (response.json \ "status" \ "name").as[String] shouldBe "bounced"

    }

    "copy relevant preferences to make payload backwards compatible" in {
      val linkSent = LocalDate.now()
      authRecordExists(nino)
      respondPreferencesWithUnverifiedEmail(Some(linkSent))
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                shouldBe 200
      (response.json \ "digital").as[Boolean]        shouldBe true
      (response.json \ "emailAddress").as[String]    shouldBe "test@email.com"
      (response.json \ "linkSent").as[String]        shouldBe linkSent.toString
      (response.json \ "status" \ "name").as[String] shouldBe "pending"
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).get()).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      stubForShutteringDisabled
      await(getRequestWithAcceptHeader(url)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl("/profile/preferences").get()).status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      await(wsUrl("/profile/preferences?journeyId=ThisIsAnInvalidJourneyId").get()).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExists(nino)

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

  "GET /profile/personal-details/:nino" should {
    val url = s"/profile/personal-details/${nino.value}?journeyId=$journeyId"

    "return 404 response status code when citizen-details returns 404 response status code." in {
      designatoryDetailsWillReturnErrorResponse(nino, 404)
      authRecordExistsNinoCheck(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 404
      response.json   shouldBe parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).get()).status shouldBe 406
    }

    "propagate 401" in {
      authFailureNinoCheck()
      stubForShutteringDisabled
      await(getRequestWithAcceptHeader(url)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl(s"/profile/personal-details/${nino.value}").get()).status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      await(wsUrl(s"/profile/personal-details/${nino.value}?journeyId=ThisIsAnInvalidJourneyId").get()).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExistsNinoCheck(nino)

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

  "POST /profile/paperless-settings/opt-in" should {
    val url      = s"/profile/preferences/paperless-settings/opt-in?journeyId=$journeyId"
    val entityId = "1098561938451038465138465"
    val paperless =
      toJson(
        Paperless(
          generic = TermsAccepted(Some(true), Some(OptInPage(Version(1, 1), 44, PageType.IosReOptInPage))),
          email   = EmailAddress("new-email@new-email.new.email"),
          Some(English)
        )
      )

    "return a 204 response when successfully opting into paperless settings" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesNoPaperlessSet()
      authRecordExists(nino)
      successPaperlessSettingsChange()
      ninoFound(nino)
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 204
    }

    "return a 204 response when a pending email preference is successfully updated" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesWithPaperlessOptedIn()
      authRecordExists(nino)
      successfulPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 204
    }

    "return a 400 response when an invalid language is sent" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesWithPaperlessOptedIn()
      authRecordExists(nino)
      successfulPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      val paperless = parse(
        """{ "email": "test@test.com", "generic": { "accepted": true, "optInPage": { "cohort": 24, "pageType": "AndroidOptInPage", "version": {"major": 1, "minor": 2 } } }, "language": "xx" }""".stripMargin
      )

      await(postRequestWithAcceptHeader(url, Json.toJson(paperless))).status shouldBe 400
    }

    "return a Conflict response when preferences has no existing verified or pending email" in {
      val expectedResponse = parse("""{"code":"CONFLICT","message":"No existing verified or pending data"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondPreferencesWithBouncedEmail()
      conflictPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, paperless))
      response.status shouldBe 409
      response.json   shouldBe expectedResponse
    }

    "return a Not Found response when unable to find a preference to update for an entity" in {
      val expectedResponse = parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      ninoFound(nino)
      respondNoPreferences()
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, paperless))
      response.status shouldBe 404
      response.json   shouldBe expectedResponse
    }

    "return a Internal Server Error response when unable update pending email preference for an entity" in {
      val expectedResponse = parse("""{"code":"PREFERENCE_SETTINGS_ERROR","message":"Failed to set preferences"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedIn()
      errorPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, paperless))
      response.status shouldBe 500
      response.json   shouldBe expectedResponse
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).post(paperless)).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      stubForShutteringDisabled
      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl("/profile/preferences/paperless-settings/opt-in").post(paperless)).status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      await(wsUrl("/profile/preferences/paperless-settings/opt-in?journeyId=ThisIsAnInvalidJourneyId").post(paperless)).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExists(nino)

      val response = await(postRequestWithAcceptHeader(url, paperless))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

  "POST /profile/paperless-settings/opt-out" should {
    val url = s"/profile/preferences/paperless-settings/opt-out?journeyId=$journeyId"
    val paperless =
      toJson(
        PaperlessOptOut(
          generic = Some(TermsAccepted(Some(false), Some(OptInPage(Version(1, 1), 44, PageType.AndroidReOptInPage)))),
          Some(English)
        )
      )

    "return a 204 response when successful" in {
      authRecordExists(nino)
      successPaperlessSettingsChange()
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 204
    }

    "return a 400 response when an invalid language is sent" in {
      authRecordExists(nino)
      successPaperlessSettingsChange()
      stubForShutteringDisabled

      val paperless = parse(
        """{ "generic": { "accepted": false, "optInPage": { "cohort": 24, "pageType": "AndroidOptOutPage", "version": {"major": 1, "minor": 2 } } }, "language": "xx" }""".stripMargin
      )

      await(postRequestWithAcceptHeader(url, Json.toJson(paperless))).status shouldBe 400
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).post(paperless)).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      stubForShutteringDisabled
      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl("/profile/preferences/paperless-settings/opt-out").post(paperless)).status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      await(wsUrl("/profile/preferences/paperless-settings/opt-out?journeyId=ThisIsAnInvalidJourneyId").post(paperless)).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExists(nino)

      val response = await(postRequestWithAcceptHeader(url, paperless))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

  "POST /profile/preferences/pending-email" should {
    val url         = s"/profile/preferences/pending-email?journeyId=$journeyId"
    val entityId    = "1098561938451038465138465"
    val changeEmail = toJson(ChangeEmail(email = EmailAddress("new-email@new-email.new.email")))

    "return a 204 response when a pending email address is successfully updated" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesWithPaperlessOptedIn()
      authRecordExists(nino)
      successfulPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, changeEmail)).status shouldBe 204
    }

    "return a Conflict response when preferences has no existing verified or pending email" in {

      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesWithPaperlessOptedIn()
      authRecordExists(nino)
      conflictPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, changeEmail))
      response.status shouldBe 409
    }

    "return a Not Found response when unable to find a preference to update for an entity" in {
      val expectedResponse = parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondNoPreferences()
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, changeEmail))
      response.status shouldBe 404
      response.json   shouldBe expectedResponse
    }

    "return a Internal Server Error response when unable update pending email preference for an entity" in {
      val expectedResponse = parse("""{"code":"PREFERENCE_SETTINGS_ERROR","message":"Failed to set preferences"}""")

      respondWithEntityDetailsByNino(nino.value, entityId)
      authRecordExists(nino)
      respondPreferencesWithPaperlessOptedIn()
      errorPendingEmailUpdate(entityId)
      ninoFound(nino)
      stubForShutteringDisabled

      val response = await(postRequestWithAcceptHeader(url, changeEmail))
      response.status shouldBe 500
      response.json   shouldBe expectedResponse
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).post(changeEmail)).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      stubForShutteringDisabled
      await(postRequestWithAcceptHeader(url, changeEmail)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl("/profile/preferences/pending-email").post(changeEmail)).status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      await(wsUrl("/profile/preferences/pending-email?journeyId=ThisIsAnInvalidJourneyId").post(changeEmail)).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExists(nino)

      val response = await(postRequestWithAcceptHeader(url, changeEmail))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

}

class CustomerProfileAllEnabledISpec extends CustomerProfileTests {
  "GET /profile/personal-details/:nino - Citizen Details Enabled" should {
    val url = s"/profile/personal-details/${nino.value}?journeyId=$journeyId"
    "return personal details for the given NINO from citizen-details" in {
      designatoryDetailsForNinoAre(nino, resourceAsString("AA000006C-citizen-details.json").get)
      authRecordExistsNinoCheck(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 200
      response.json   shouldBe getResourceAsJsValue("expected-AA000006C-personal-details.json")
    }

    "return a 423 response status code when the NINO is locked due to Manual Correspondence Indicator flag being set in NPS" in {
      npsDataIsLockedDueToMciFlag(nino)
      authRecordExistsNinoCheck(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 423
      response.json shouldBe parse(
        """{"code":"MANUAL_CORRESPONDENCE_IND","message":"Data cannot be disclosed to the user because MCI flag is set in NPS"}"""
      )
    }

    "return 500 response status code when citizen-details returns 500 response status code." in {
      designatoryDetailsWillReturnErrorResponse(nino, 500)
      authRecordExistsNinoCheck(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 500
      response.json   shouldBe parse("""{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}""")
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExistsNinoCheck(nino)

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }
  }

}

class CustomerProfileCitizenDetailsDisabledISpec extends CustomerProfileTests {

  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    config ++
    Map(
      "microservice.services.citizen-details.enabled" -> false
    )
  )

  "GET /profile/personal-details/:nino - Citizen Details Disabled" should {
    val url = s"/profile/personal-details/${nino.value}?journeyId=$journeyId"
    "return 404 for disabled citizen-details" in {
      authRecordExistsNinoCheck(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 404
    }

  }
}

class CustomerProfilePaperlessVersionsEnabledISpec extends CustomerProfileTests {

  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    config ++
    Map(
      "optInVersionsEnabled" -> true
    )
  )

  "POST /profile/paperless-settings/opt-in - Paperless Versions Enabled" should {
    val url      = s"/profile/preferences/paperless-settings/opt-in?journeyId=$journeyId"
    val entityId = "1098561938451038465138465"
    val paperless =
      toJson(
        Paperless(
          generic = TermsAccepted(accepted = Some(true), Some(OptInPage(Version(1, 1), 44, PageType.IosOptInPage))),
          email   = EmailAddress("new-email@new-email.new.email"),
          Some(English)
        )
      )

    "return a 204 response and send version info when successfully opting into paperless settings" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesNoPaperlessSet()
      authRecordExists(nino)
      successPaperlessSettingsOptInWithVersion
      ninoFound(nino)
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 204
    }

  }

  "POST /profile/paperless-settings/opt-out - Paperless Versions Enabled" should {
    val url      = s"/profile/preferences/paperless-settings/opt-out?journeyId=$journeyId"
    val entityId = "1098561938451038465138465"
    val paperless =
      toJson(
        Paperless(
          generic = TermsAccepted(accepted = Some(false), Some(OptInPage(Version(1, 1), 44, PageType.IosOptOutPage))),
          email   = EmailAddress("new-email@new-email.new.email"),
          Some(English)
        )
      )

    "return a 204 response and send version info when successfully opting into paperless settings" in {
      respondWithEntityDetailsByNino(nino.value, entityId)
      respondPreferencesNoPaperlessSet()
      authRecordExists(nino)
      successPaperlessSettingsOptOutWithVersion
      ninoFound(nino)
      stubForShutteringDisabled

      await(postRequestWithAcceptHeader(url, paperless)).status shouldBe 204
    }

  }
}

class CustomerProfileReOptInDisabledISpec extends CustomerProfileTests {

  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    config ++
    Map(
      "reOptInEnabled" -> false
    )
  )

  "GET /profile/preferences" should {
    val url = s"/profile/preferences?journeyId=$journeyId"

    "return preferences with a status of verified instead of reOptIn" in {
      authRecordExists(nino)
      respondPreferencesWithReOptInRequired()
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                     shouldBe 200
      (response.json \ "digital").as[Boolean]             shouldBe true
      (response.json \ "status" \ "name").as[String]      shouldBe "verified"
      (response.json \ "status" \ "majorVersion").as[Int] shouldBe 10
    }

    "treat RE_OPT_IN_MODIFIED as ReOpt in require" in {
      authRecordExists(nino)
      respondPreferencesWithReOptInModified()
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                     shouldBe 200
      (response.json \ "digital").as[Boolean]             shouldBe true
      (response.json \ "status" \ "name").as[String]      shouldBe "verified"
      (response.json \ "status" \ "majorVersion").as[Int] shouldBe 10
    }

  }
}
