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

import play.api.libs.json.Json.toJson
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.customerprofile.domain.StatusName.{Bounced, Pending, ReOptIn, Verified}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.*
import uk.gov.hmrc.customerprofile.domain.types.JourneyId
import uk.gov.hmrc.customerprofile.support.BaseISpec
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.customerprofile.emailaddress.EmailAddress

import java.time.LocalDate

class SandboxCustomerProfileISpec extends BaseISpec {

  def request(
    url: String,
    sandboxControl: Option[String] = None,
    journeyId: JourneyId
  ): WSRequest =
    wsUrl(s"$url?journeyId=${journeyId.value}")
      .addHttpHeaders(
        acceptJsonHeader,
        "SANDBOX-CONTROL"  -> s"${sandboxControl.getOrElse("")}",
        "X-MOBILE-USER-ID" -> "208606423740"
      )

  def requestWithoutAcceptHeader(
    url: String,
    journeyId: JourneyId
  ): WSRequest =
    wsUrl(s"$url?journeyId=${journeyId.value}")
      .addHttpHeaders("X-MOBILE-USER-ID" -> "208606423740")

  def requestWithoutJourneyId(
    url: String,
    sandboxControl: Option[String] = None
  ): WSRequest =
    wsUrl(s"$url").addHttpHeaders(
      acceptJsonHeader,
      "SANDBOX-CONTROL"  -> s"${sandboxControl.getOrElse("")}",
      "X-MOBILE-USER-ID" -> "208606423740"
    )

  "GET /sandbox/profile/personal-details/:nino" should {
    val url = s"/profile/personal-details/${nino.value}"

    val expectedDetails = """{
                            |  "person" : {
                            |    "firstName" : "Nia",
                            |    "lastName" : "Jackson",
                            |    "title" : "Ms",
                            |    "sex" : "Female",
                            |    "personDateOfBirth" : "1999-01-31",
                            |    "nino" : "QQ123456C",
                            |    "fullName" : "Nia Jackson",
                            |    "nationalInsuranceLetterUrl" : "/"
                            |  },
                            |  "address" : {
                            |    "line1" : "999 Big Street",
                            |    "line2" : "Worthing",
                            |    "line3" : "West Sussex",
                            |    "postcode" : "BN99 8IG",
                            |    "changeAddressLink" : "/"
                            |  },
                            |  "correspondenceAddress" : {
                            |    "line1" : "1 Main Street",
                            |    "line2" : "Brighton",
                            |    "line3" : "East Sussex",
                            |    "postcode" : "BN1 1AA"
                            |  }
                            |}""".stripMargin

    "return the default personal details with journey id" in {
      val response = await(request(url, None, journeyId).get())
      response.status                 shouldBe 200
      Json.prettyPrint(response.json) shouldBe expectedDetails
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401"), journeyId).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403"), journeyId).get())
      response.status shouldBe 403
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, journeyId).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500"), journeyId).get())
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          s"/profile/personal-details/${nino.value}",
          None
        ).get()
      )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          s"/profile/personal-details/${nino.value}?journeyId=ThisIsAnInvalidJourneyId",
          None
        ).get()
      )
      response.status shouldBe 400
    }
  }

  "GET /sandbox/profile/preferences" should {
    val url = "/profile/preferences"
    val expectedPreference =
      Preference(
        digital = false,
        None
      )

    def preferencesSandbox(
      status: StatusName,
      linkSent: Option[LocalDate] = None
    ) =
      Preference(
        digital      = true,
        emailAddress = Some("jt@test.com"),
        status =
          if (status == ReOptIn) Some(PaperlessStatus(status, Category.ActionRequired, Some(10)))
          else Some(PaperlessStatus(status, Category.ActionRequired)),
        linkSent = linkSent
      )

    "return the default preferences with a journeyId" in {
      val response = await(request(url, None, journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(expectedPreference)
    }

    "return the verified preferences for VERIFIED" in {
      val response = await(request(url, Some("VERIFIED"), journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Verified))
    }

    "return the unverified preferences for UNVERIFIED" in {
      val response = await(request(url, Some("UNVERIFIED"), journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Pending, Some(LocalDate.now())))
    }

    "return the bounced preferences for BOUNCED" in {
      val response = await(request(url, Some("BOUNCED"), journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Bounced))
    }

    "return the reOptIn preferences for REOPTIN" in {
      val response = await(request(url, Some("REOPTIN"), journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(ReOptIn))
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401"), journeyId).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403"), journeyId).get())
      response.status shouldBe 403
    }

    "return 403 for ERROR-404" in {
      val response = await(request(url, Some("ERROR-404"), journeyId).get())
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, journeyId).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500"), journeyId).get())
      response.status shouldBe 500
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).get()
        )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-in" should {
    val url = "/profile/preferences/paperless-settings/opt-in"
    val paperlessSettings =
      toJson(
        Paperless(
          generic = TermsAccepted(Some(true)),
          email   = EmailAddress("new-email@new-email.new.email"),
          Some(English)
        )
      )

    "return a 204 response with a journeyId by default" in {
      val response =
        await(request(url, None, journeyId).post(paperlessSettings))
      response.status shouldBe 204
    }

    "return a 201 response for PREFERENCE-CREATED" in {
      val response = await(
        request(url, Some("PREFERENCE-CREATED"), journeyId)
          .post(paperlessSettings)
      )
      response.status shouldBe 201
    }

    "return 401 for ERROR-401" in {
      val response = await(
        request(url, Some("ERROR-401"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(
        request(url, Some("ERROR-403"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response = await(
        request(url, Some("ERROR-404"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 404
    }

    "return a 409 response for ERROR-409" in {
      val response = await(
        request(url, Some("ERROR-409"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 409
    }

    "return a 500 response for ERROR-500" in {
      val response = await(
        request(url, Some("ERROR-500"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          "/profile/preferences/paperless-settings/opt-in?journeyId=ThisIsAnInvalidJourneyId",
          None
        ).post(paperlessSettings)
      )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-out" should {
    val url = "/profile/preferences/paperless-settings/opt-out"

    val paperlessSettings =
      toJson(
        PaperlessOptOut(
          generic = Some(TermsAccepted(Some(false))),
          Some(English)
        )
      )

    "return a 204 response by default with journeyId" in {
      val response = await(request(url, None, journeyId).post(paperlessSettings))
      response.status shouldBe 204
    }

    "return 401 for ERROR-401" in {
      val response =
        await(request(url, Some("ERROR-401"), journeyId).post(paperlessSettings))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response =
        await(request(url, Some("ERROR-403"), journeyId).post(paperlessSettings))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response =
        await(request(url, Some("ERROR-404"), journeyId).post(paperlessSettings))
      response.status shouldBe 404
    }

    "return a 500 response for ERROR-500" in {
      val response =
        await(request(url, Some("ERROR-500"), journeyId).post(paperlessSettings))
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-out",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-out?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/profile/preferences/pending-email" should {
    val url = "/profile/preferences/pending-email"
    val changeEmail =
      toJson(ChangeEmail(email = EmailAddress("new-email@new-email.new.email")))

    "return a 204 response with a journeyId by default" in {
      val response = await(request(url, None, journeyId).post(changeEmail))
      response.status shouldBe 204
    }

    "return 401 for ERROR-401" in {
      val response =
        await(request(url, Some("ERROR-401"), journeyId).post(changeEmail))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response =
        await(request(url, Some("ERROR-403"), journeyId).post(changeEmail))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response =
        await(request(url, Some("ERROR-404"), journeyId).post(changeEmail))
      response.status shouldBe 404
    }

    "return a 409 response for ERROR-409" in {
      val response =
        await(request(url, Some("ERROR-409"), journeyId).post(changeEmail))
      response.status shouldBe 409
    }

    "return a 500 response for ERROR-500" in {
      val response =
        await(request(url, Some("ERROR-500"), journeyId).post(changeEmail))
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in",
            None
          ).post(changeEmail)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).post(changeEmail)
        )
      response.status shouldBe 400
    }
  }

}
