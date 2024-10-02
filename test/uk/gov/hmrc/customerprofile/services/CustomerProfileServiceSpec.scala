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

package uk.gov.hmrc.customerprofile.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.Configuration
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals

import scala.concurrent.duration._
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, EmailUpdateOk, Entity, EntityResolverConnector, PreferencesConnector, PreferencesCreated, PreferencesExists, PreferencesStatus}
import uk.gov.hmrc.customerprofile.domain.StatusName.{ReOptIn, Verified}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}

class CustomerProfileServiceSpec extends BaseSpec {

  val appNameConfiguration: Configuration  = mock[Configuration]
  val mockCitizenDetailsConnector: CitizenDetailsConnector =
    mock[CitizenDetailsConnector]
  val mockPreferencesConnector: PreferencesConnector    = mock[PreferencesConnector]
  val mockEntityResolver:       EntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthRetrievals: AuthRetrievals          = mock[AuthRetrievals]
  val mockAuditService:         AuditService            = mock[AuditService]

  val service =
    new CustomerProfileService(
      mockCitizenDetailsConnector,
      mockPreferencesConnector,
      mockEntityResolver,
      mockAuthRetrievals,
      "customer-profile",
      true,
      mockAuditService
    )

  implicit val defaultTimeout: FiniteDuration = 5 seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)


    def mockGetAccounts() = {
      when(mockAuditService.withAudit[Option[Nino]](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(Some(nino)))
      when(mockAuthRetrievals.retrieveNino()(any(), any())).thenReturn(Future.successful(Some(nino)))
  }

  def mockGetEntityIdAndUpdatePendingEmailWithAudit() = {

    when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
      .thenReturn(Future.successful(EmailUpdateOk))
    when(mockEntityResolver.getEntityIdByNino(any())(any(),any())).thenReturn(Future.successful(entity))
    when(mockPreferencesConnector.updatePendingEmail(any(), any())(any(), any())).thenReturn(Future.successful(EmailUpdateOk))
  }

  "getPersonalDetails" should {
    "audit and return accounts" in {
      val person = PersonDetails(
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
          Some("/save-your-national-insurance-number/print-letter/save-letter-as-pdf")
        ),
        None,
        None
      )

      val updatedPerson = person.copy(address =
        Some(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
      )
      when(mockCitizenDetailsConnector.personDetails(any())(any(), any())).thenReturn(Future.successful(person))
      when(mockAuditService.withAudit[PersonDetails](any(), any())(any())(any(), any())).thenReturn(Future.successful(updatedPerson))

      val personalDetails = await(service.getPersonalDetails(nino))

      personalDetails mustBe person.copy(address =
        Some(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
      )
      personalDetails.person.shortName mustBe "Firstname Lastname"
      personalDetails.person.completeName mustBe "Title Firstname Lastname Honours"
    }

    "return middle name if present" in {

      val updatedPerson = person3.copy(address =
        Some(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
      )
      when(mockCitizenDetailsConnector.personDetails(any())(any(), any())).thenReturn(Future.successful(person3))
      when(mockAuditService.withAudit[PersonDetails](any(), any())(any())(any(), any())).thenReturn(Future.successful(updatedPerson))

      val personalDetails = await(service.getPersonalDetails(nino))
      personalDetails mustBe updatedPerson
      personalDetails.person.shortName mustBe "Firstname Middlename Lastname"
      personalDetails.person.completeName mustBe "Title Firstname Middlename Lastname Honours"
    }
  }

  "getPreferences" should {
    "audit and return preferences" in {

      val updatedPref =  existingDigitalPreference.copy(
        emailAddress = existingDigitalPreference.email.map(_.email.value),
        status = Some(
          PaperlessStatus(existingDigitalPreference.status.get.name, category = Category.Info)
        ),
        email = None
      )

      when(mockAuditService.withAudit[Option[Preference]](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(Some(updatedPref)))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(Some(existingDigitalPreference)))

      await(service.getPreferences()) mustBe Some(updatedPref)
    }
    "audit and return preferences if signed out" in {
      val preferences = existingDigitalPreference.copy(digital = false)

      val updatedPref = Some(preferences.copy(
        emailAddress = preferences.email.map(_.email.value),
        email        = None
      ))
      when(mockAuditService.withAudit[Option[Preference]](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(updatedPref))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(Some(preferences)))

      await(service.getPreferences()) mustBe updatedPref

    }
 }

  "paperlessSettings" should {

    "ReOptIn for a user who already has a defined digital preference and has received the reOptIn status" in {

      val updatedPref = Some(
        existingDigitalPreference.copy(status = Some(PaperlessStatus(StatusName.ReOptIn, Category.ReOptInRequired)))
      )
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(updatedPref))
      when(mockEntityResolver.paperlessSettings(any())(any(),any())).thenReturn(Future.successful(PreferencesExists))
      val result = await(service.paperlessSettings(newPaperlessSettings, journeyId))
      result  mustBe PreferencesExists
    }

    "update the email for a user who already has a defined digital preference" in {

      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(EmailUpdateOk))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(Some(existingDigitalPreference)))
      mockGetAccounts()
      mockGetEntityIdAndUpdatePendingEmailWithAudit()

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) mustBe  EmailUpdateOk
    }

    "set the digital preference to true and update the email for a user who already has a defined non-digital preference" in {
      reset(mockEntityResolver)
      reset(mockAuditService)
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(Some(existingDigitalPreference)))
      when(mockEntityResolver.paperlessSettings(any())(any(),any())).thenReturn(Future.successful(PreferencesExists))

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) mustBe PreferencesExists
    }

    "set the digital preference to true and set the email for a user who has no defined preference" in {
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(None))
      when(mockEntityResolver.paperlessSettings(any())(any(),any())).thenReturn(Future.successful(PreferencesCreated))

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) mustBe  PreferencesCreated
    }

    "If sent, override the accepted value to 'true' when opting in" in {
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))
      when(mockEntityResolver.getPreferences()(any(),any())).thenReturn(Future.successful(None))
      when(mockEntityResolver.paperlessSettings(any())(any(),any())).thenReturn(Future.successful(PreferencesCreated))

      await(
        service.paperlessSettings(newPaperlessSettings
                                    .copy(generic = newPaperlessSettings.generic.copy(accepted = Some(false))),
                                  journeyId)
      ) mustBe PreferencesCreated
    }
  }

  "paperlessSettingsOptOut" should {
    "audit and opt the user out" in {
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))
      when(mockEntityResolver.paperlessOptOut(any())(any(), any())).thenReturn(Future.successful(PreferencesExists))
      await(service.paperlessSettingsOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English)))) mustBe  PreferencesExists
    }

    "If sent, override the accepted value to 'false' when opting out" in {

      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))
      when(mockEntityResolver.paperlessOptOut(any())(any(), any())).thenReturn(Future.successful(PreferencesExists))

      await(service.paperlessSettingsOptOut(PaperlessOptOut(Some(TermsAccepted(Some(true))), Some(English)))) mustBe PreferencesExists
    }
  }

  "reOptInEnabledCheck" should {
    "pass through ReOptIn status if enabled" in {
      val expectedPreferences = preferencesWithStatus(ReOptIn)

      service.reOptInEnabledCheck(expectedPreferences) mustBe
        expectedPreferences.copy(
          status = Some(
            PaperlessStatus(ReOptIn, category = Category.Info)
          )
        )
    }
    "replace ReOptIn status with Verified if disabled" in {
      val service =
        new CustomerProfileService(
          mockCitizenDetailsConnector,
          mockPreferencesConnector,
          mockEntityResolver,
          mockAuthRetrievals,
          "customer-profile",
          false,
          mockAuditService
        )
      val expectedPreferences = preferencesWithStatus(ReOptIn)

      service.reOptInEnabledCheck(expectedPreferences) mustBe
        expectedPreferences.copy(
          status = Some(
            PaperlessStatus(Verified, category = Category.Info)
          )
        )
    }
  }

}
