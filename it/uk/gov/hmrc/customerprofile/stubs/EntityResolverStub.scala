package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.libs.json.Json.{stringify, toJson}
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status._
import uk.gov.hmrc.customerprofile.domain.{EmailPreference, Preference}
import uk.gov.hmrc.emailaddress.EmailAddress

object EntityResolverStub {

  private def entityDetailsByNino(nino: String, entityId: String): String = s"""
                                       |{
                                       |  "_id":"$entityId",
                                       |  "sautr":"8040200778",
                                       |  "nino":"$nino"
                                       |}""".stripMargin

  private def preferences(optedIn: Boolean = true, email: String = "test@email.com", status: Status = Verified): Preference = {
    if(optedIn) {
      Preference(optedIn, Some(EmailPreference(EmailAddress(email), status)))
    }
    else Preference(false)
  }

  private def urlEqualToEntityResolverPaye(nino: String): UrlPattern = {
    urlEqualTo(s"/entity-resolver/paye/${nino}")
  }

  def respondWithEntityDetailsByNino(nino: String, entityId: String): Unit =
  stubFor(get(urlEqualToEntityResolverPaye(nino))
    .willReturn(aResponse().withStatus(200).withBody(entityDetailsByNino(nino, entityId))))

  def respondPreferencesWithPaperlessOptedIn(): Unit = {
    stubFor(get(urlEqualToPreferences).willReturn(aResponse().withStatus(200).withBody(stringify(toJson(preferences())))))
  }

  def respondPreferencesWithBouncedEmail(): Unit = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse()
        .withStatus(200).withBody(stringify(toJson(preferences(optedIn = true, status = Bounced))))))
  }

  def respondPreferencesNoPaperlessSet(): Unit = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse().withStatus(200).withBody(stringify(toJson(preferences(optedIn = false))))))
  }

  def respondNoPreferences(): Unit = {
    stubFor(get(urlEqualToPreferences).willReturn(aResponse().withStatus(404)))
  }

  def successPaperlessSettingsChange(): Unit = {
    stubFor(post(urlEqualToPaperlessSettingsChange).willReturn(aResponse().withStatus(200)))
  }

  val urlEqualToPreferences: UrlPattern = urlEqualTo(s"/preferences")
  val urlEqualToPaperlessSettingsChange: UrlPattern = urlEqualTo(s"/preferences/terms-and-conditions")
}
