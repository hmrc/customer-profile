package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.StatusName.{Bounced, Pending, Verified}
import uk.gov.hmrc.customerprofile.domain.*
import uk.gov.hmrc.emailaddress.EmailAddress

import java.time.LocalDate

object EntityResolverStub {

  private def entityDetailsByNino(
    nino: String,
    entityId: String
  ): String = s"""
                 |{
                 |  "_id":"$entityId",
                 |  "sautr":"8040200778",
                 |  "nino":"$nino"
                 |}""".stripMargin

  private def preferences(
    optedIn: Boolean = true,
    email: String = "test@email.com",
    emailStatus: StatusName = Verified,
    status: String = "ALRIGHT",
    linkSent: Option[LocalDate] = None
  ): String =
    if (optedIn) {
      s"""
         |{
         |  "digital" : true,
         |  "emailAddress": "$email",
         |  "linkSent": "${linkSent.getOrElse(LocalDate.now())}",
         |  "email" : {
         |    "email" : "$email",
         |    "status" : "$status",
         |    "linkSent": "${linkSent.getOrElse(LocalDate.now())}"
         |  },
         |  "status": {
         |      "name": "$status",
         |      "category": "ACTION_REQUIRED",
         |      "reoptinMajor": 10
         |  }
         |}
      """.stripMargin
    } else
      s"""
         |{
         |  "digital" : false,
         |  "status": {
         |      "name": "$status",
         |      "category": "OPT_IN_REQUIRED"
         |  }
         |}
      """.stripMargin

  private def urlEqualToEntityResolverPaye(nino: String): UrlPattern =
    urlEqualTo(s"/entity-resolver/paye/$nino")

  def respondWithEntityDetailsByNino(
    nino: String,
    entityId: String
  ): StubMapping =
    stubFor(
      get(urlEqualToEntityResolverPaye(nino))
        .willReturn(aResponse().withStatus(200).withBody(entityDetailsByNino(nino, entityId)))
    )

  def respondPreferencesWithPaperlessOptedIn(): StubMapping =
    stubFor(
      get(urlEqualToPreferences).willReturn(aResponse().withStatus(200).withBody(preferences()))
    )

  def respondPreferencesWithPaperlessOptedOut(): StubMapping =
    stubFor(
      get(urlEqualToPreferences).willReturn(
        aResponse().withStatus(200).withBody(preferences(optedIn = false, status = "PAPER"))
      )
    )

  def respondPreferencesWithBouncedEmail(): StubMapping =
    stubFor(
      get(urlEqualToPreferences)
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(preferences(emailStatus = Bounced, status = "BOUNCED_EMAIL"))
        )
    )

  def respondPreferencesWithUnverifiedEmail(linkSent: Option[LocalDate] = None): StubMapping =
    stubFor(
      get(urlEqualToPreferences)
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              preferences(emailStatus = Pending, status = "EMAIL_NOT_VERIFIED", linkSent = linkSent)
            )
        )
    )

  def respondPreferencesWithReOptInRequired(): StubMapping =
    stubFor(
      get(urlEqualToPreferences).willReturn(
        aResponse().withStatus(200).withBody(preferences(status = "OLD_VERSION"))
      )
    )

  def respondPreferencesWithReOptInModified(): StubMapping =
    stubFor(
      get(urlEqualToPreferences).willReturn(
        aResponse().withStatus(200).withBody(preferences(status = "RE_OPT_IN_MODIFIED"))
      )
    )

  def respondPreferencesNoPaperlessSet(): StubMapping =
    stubFor(
      get(urlEqualToPreferences)
        .willReturn(aResponse().withStatus(200).withBody(preferences(optedIn = false)))
    )

  def respondNoPreferences(): StubMapping =
    stubFor(get(urlEqualToPreferences).willReturn(aResponse().withStatus(404)))

  def successPaperlessSettingsChange(): StubMapping =
    stubFor(post(urlEqualToPaperlessSettingsChange).willReturn(aResponse().withStatus(200)))

  def successPaperlessSettingsOptInWithVersion: StubMapping =
    stubFor(
      post(urlEqualToPaperlessSettingsChange)
        .withRequestBody(
          equalToJson(
            Json
              .toJson(
                Paperless(
                  generic = TermsAccepted(Some(true), Some(OptInPage(Version(1, 1), 44, PageType.IosOptInPage))),
                  email   = EmailAddress("new-email@new-email.new.email"),
                  Some(English)
                )
              )
              .toString(),
            true,
            false
          )
        )
        .willReturn(aResponse().withStatus(200))
    )

  def successPaperlessSettingsOptOutWithVersion: StubMapping =
    stubFor(
      post(urlEqualToPaperlessSettingsChange)
        .withRequestBody(
          equalToJson(
            Json
              .toJson(
                PaperlessOptOut(generic = Some(
                                  TermsAccepted(Some(false), Some(OptInPage(Version(1, 1), 44, PageType.IosOptOutPage)))
                                ),
                                Some(English)
                               )
              )
              .toString(),
            true,
            false
          )
        )
        .willReturn(aResponse().withStatus(200))
    )

  val urlEqualToPreferences: UrlPattern = urlEqualTo(s"/preferences")
  val urlEqualToPaperlessSettingsChange: UrlPattern = urlEqualTo(s"/preferences/terms-and-conditions")
}
