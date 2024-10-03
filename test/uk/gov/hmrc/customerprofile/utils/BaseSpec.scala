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

package uk.gov.hmrc.customerprofile.utils

import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import eu.timepit.refined.auto._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite}
import play.api.libs.json.Json.toJson
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.customerprofile.connector.Entity
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.StatusName.Verified
import uk.gov.hmrc.customerprofile.domain.{Address, Category, EmailPreference, OptInPage, PageType, Paperless, PaperlessStatus, Person, PersonDetails, Preference, Shuttering, StatusName, TermsAccepted, Version}

import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

trait BaseSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val config:             Configuration  = mock[Configuration]
  val environment:        Environment    = mock[Environment]
  val mockHttpClient:     HttpClientV2   = mock[HttpClientV2]

  type GrantAccess = Option[String] ~ ConfidenceLevel

  implicit val mockAuthConnector: AuthConnector        = mock[AuthConnector]
  lazy val components:            ControllerComponents = app.injector.instanceOf[ControllerComponents]

  implicit lazy val hc: HeaderCarrier    = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val appName:              String      = "customer-profile"
  val nino:                 Nino        = Nino("CS700100A")
  val journeyId:            JourneyId   = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val acceptheader:         String      = "application/vnd.hmrc.1.0+json"
  val grantAccessWithCL200: GrantAccess = Some(nino.nino) and L200
  val entity:               Entity      = Entity("entityId")

  val person = PersonDetails(
    Person(
      Some("Firstname"),
      Some("Middle"),
      Some("Lastname"),
      Some("Intial"),
      Some("Title"),
      Some("Honours"),
      Some("sex"),
      None,
      None,
      Some("Firstname Lastname"),
      Some("/personal-account/national-insurance-summary/save-letter-as-pdf")
    ),
    Some(Address(changeAddressLink = Some("/personal-account/your-profile"))),
    None
  )

  val person2: PersonDetails = PersonDetails(
    Person(
      Some("Firstname"),
      None,
      Some("Lastname"),
      Some("Intial"),
      Some("Title"),
      Some("Honours"),
      Some("sex"),
      None,
      None,
      Some("Firstname Lastname"),
      Some("/personal-account/national-insurance-summary/save-letter-as-pdf")
    ),
    None,
    None
  )

  val person3 = PersonDetails(
    Person(
      Some("Firstname"),
      Some("Middlename"),
      Some("Lastname"),
      Some("Intial"),
      Some("Title"),
      Some("Honours"),
      Some("sex"),
      None,
      None,
      Some("Firstname Middlename Lastname"),
      Some("/save-your-national-insurance-number/print-letter/save-letter-as-pdf")
    ),
    None,
    None
  )

  val shuttered: Shuttering =
    Shuttering(
      shuttered = true,
      Some("Shuttered"),
      Some("Customer-Profile is currently not available")
    )
  val notShuttered: Shuttering = Shuttering.shutteringDisabled

  val requestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Accept" -> acceptheader)
  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val requestWithoutAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Authorization" -> "Some Header")

  val invalidPostRequest: FakeRequest[JsValue] =
    FakeRequest()
      .withBody(Json.parse("""{ "blah" : "blah" }"""))
      .withHeaders(HeaderNames.ACCEPT -> acceptheader)

  val newEmail          = EmailAddress("new@new.com")
  val paperlessSettings = Paperless(TermsAccepted(Some(true)), newEmail, Some(English))

  val paperlessSettingsWithVersion =
    Paperless(TermsAccepted(accepted = Some(true), Some(OptInPage(Version(1, 1), 44, PageType.AndroidOptInPage))),
              newEmail,
              Some(English))

  val validPaperlessSettingsRequest: FakeRequest[JsValue] =
    FakeRequest()
      .withBody(toJson(paperlessSettingsWithVersion))
      .withHeaders(HeaderNames.ACCEPT -> acceptheader)

  val paperlessSettingsRequestWithoutAcceptHeader: FakeRequest[JsValue] =
    FakeRequest().withBody(toJson(paperlessSettings))

  def preferencesWithStatus(status: StatusName): Preference = existingPreferences(
    digital = true,
    status
  )

  val existingDigitalPreference: Preference = existingPreferences(
    digital = true
  )

  val existingNonDigitalPreference: Preference = existingPreferences(
    digital = false
  )

  val newPaperlessSettings = Paperless(TermsAccepted(Some(true)), newEmail, Some(English))

  def existingPreferences(
    digital: Boolean,
    status:  StatusName = Verified
  ): Preference =
    Preference(
      digital = digital,
      email   = Some(EmailPreference(EmailAddress("old@old.com"), status)),
      status  = Some(PaperlessStatus(name = status, category = Category.Info))
    )

}
