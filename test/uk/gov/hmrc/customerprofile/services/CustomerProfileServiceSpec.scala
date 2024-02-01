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

package uk.gov.hmrc.customerprofile.services

import org.scalamock.handlers.CallHandler3
import org.scalamock.matchers.MatcherBase
import play.api.Configuration
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, EmailUpdateOk, Entity, EntityResolverConnector, PreferencesConnector, PreferencesCreated, PreferencesExists, PreferencesStatus}
import uk.gov.hmrc.customerprofile.domain.StatusName.{ReOptIn, Verified}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}

class CustomerProfileServiceSpec extends BaseSpec {

  val appNameConfiguration: Configuration  = mock[Configuration]
  val auditConnector:       AuditConnector = mock[AuditConnector]

  def mockAudit(
    transactionName: String,
    detail:          Map[String, String] = Map.empty
  ): CallHandler3[DataEvent, HeaderCarrier, ExecutionContext, Future[
    AuditResult
  ]] = {
    def dataEventWith(
      auditSource: String,
      auditType:   String,
      tags:        Map[String, String]
    ): MatcherBase =
      argThat { (dataEvent: DataEvent) =>
        dataEvent.auditSource.equals(auditSource) &&
        dataEvent.auditType.equals(auditType) &&
        dataEvent.tags.equals(tags) &&
        dataEvent.detail.equals(detail)
      }

    (auditConnector
      .sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        dataEventWith(
          appName,
          auditType = "ServiceResponseSent",
          tags      = Map("transactionName" -> transactionName)
        ),
        *,
        *
      )
      .returns(Future successful Success)
  }

  val citizenDetailsConnector: CitizenDetailsConnector =
    mock[CitizenDetailsConnector]
  val preferencesConnector: PreferencesConnector    = mock[PreferencesConnector]
  val entityResolver:       EntityResolverConnector = mock[EntityResolverConnector]
  val accountAccessControl: AuthRetrievals          = mock[AuthRetrievals]
  val auditService:         AuditService            = new AuditService(auditConnector, "customer-profile")

  val service =
    new CustomerProfileService(
      citizenDetailsConnector,
      preferencesConnector,
      entityResolver,
      accountAccessControl,
      auditConnector,
      "customer-profile",
      true,
      auditService
    )

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

  val newEmail             = EmailAddress("new@new.com")
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

  def mockGetAccounts() = {
    mockAudit(transactionName = "getAccounts")
    (accountAccessControl
      .retrieveNino()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful Some(nino))
  }

  def mockGetPreferences(maybeExistingPreferences: Option[Preference]) =
    (entityResolver
      .getPreferences()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful maybeExistingPreferences)

  def mockAuditPaperlessSettings() =
    mockAudit(
      transactionName = "paperlessSettings",
      detail          = Map("accepted" -> newPaperlessSettings.generic.accepted.toString)
    )

  def mockPaperlessSettings(status: PreferencesStatus) =
    (entityResolver
      .paperlessSettings(_: Paperless)(_: HeaderCarrier, _: ExecutionContext))
      .expects(newPaperlessSettings, *, *)
      .returns(Future successful status)

  def mockGetEntityIdAndUpdatePendingEmailWithAudit() = {
    val entity: Entity = Entity("entityId")

    mockAudit(
      transactionName = "updatePendingEmailPreference",
      detail          = Map("email" -> newEmail.value)
    )
    (entityResolver
      .getEntityIdByNino(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returns(Future successful entity)

    (preferencesConnector
      .updatePendingEmail(_: ChangeEmail, _: String)(
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(ChangeEmail(newEmail.value), entity._id, *, *)
      .returns(Future.successful(EmailUpdateOk))
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

      mockAudit(
        transactionName = "getPersonalDetails",
        detail          = Map("nino" -> nino.value)
      )
      (citizenDetailsConnector
        .personDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *)
        .returns(Future successful person)
      val personalDetails = await(service.getPersonalDetails(nino))

      personalDetails shouldBe person.copy(address =
        Some(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
      )
      personalDetails.person.shortName    shouldBe "Firstname Lastname"
      personalDetails.person.completeName shouldBe "Title Firstname Lastname Honours"
    }

    "return middle name if present" in {
      val person = PersonDetails(
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

      mockAudit(
        transactionName = "getPersonalDetails",
        detail          = Map("nino" -> nino.value)
      )
      (citizenDetailsConnector
        .personDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *)
        .returns(Future successful person)
      val personalDetails = await(service.getPersonalDetails(nino))

      personalDetails shouldBe person.copy(address =
        Some(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
      )
      personalDetails.person.shortName    shouldBe "Firstname Middlename Lastname"
      personalDetails.person.completeName shouldBe "Title Firstname Middlename Lastname Honours"
    }
  }

  "getPreferences" should {
    "audit and return preferences" in {
      mockAudit(transactionName = "getPreferences")
      mockGetPreferences(Some(existingDigitalPreference))

      await(service.getPreferences()) shouldBe Some(
        existingDigitalPreference.copy(
          emailAddress = existingDigitalPreference.email.map(_.email.value),
          status = Some(
            PaperlessStatus(existingDigitalPreference.status.get.name, category = Category.Info)
          ),
          email = None
        )
      )
    }
    "audit and return preferences if signed out" in {
      val preferences = existingDigitalPreference.copy(digital = false)
      mockAudit(transactionName = "getPreferences")
      mockGetPreferences(Some(preferences))

      await(service.getPreferences()) shouldBe Some(
        preferences.copy(
          emailAddress = preferences.email.map(_.email.value),
          email        = None
        )
      )

    }
  }

  "paperlessSettings" should {
    "update the email for a user who already has a defined digital preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(Some(existingDigitalPreference))
      mockGetAccounts()
      mockGetEntityIdAndUpdatePendingEmailWithAudit()

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe EmailUpdateOk
    }

    "ReOptIn for a user who already has a defined digital preference and has received the reOptIn status" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(
        Some(
          existingDigitalPreference.copy(status = Some(PaperlessStatus(StatusName.ReOptIn, Category.ReOptInRequired)))
        )
      )
      mockPaperlessSettings(PreferencesExists)

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe PreferencesExists
    }

    "set the digital preference to true and update the email for a user who already has a defined non-digital preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(Some(existingNonDigitalPreference))
      mockPaperlessSettings(PreferencesExists)

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe PreferencesExists
    }

    "set the digital preference to true and set the email for a user who has no defined preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(None)
      mockPaperlessSettings(PreferencesCreated)

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe PreferencesCreated
    }

    "If sent, override the accepted value to 'true' when opting in" in {
      mockAudit(
        transactionName = "paperlessSettings",
        detail          = Map("accepted" -> "Some(false)")
      )
      mockGetPreferences(None)
      mockPaperlessSettings(PreferencesCreated)

      await(
        service.paperlessSettings(newPaperlessSettings
                                    .copy(generic = newPaperlessSettings.generic.copy(accepted = Some(false))),
                                  journeyId)
      ) shouldBe PreferencesCreated
    }
  }

  "paperlessSettingsOptOut" should {
    "audit and opt the user out" in {
      mockAudit(transactionName = "paperlessSettingsOptOut")
      (entityResolver
        .paperlessOptOut(_: PaperlessOptOut)(_: HeaderCarrier, _: ExecutionContext))
        .expects(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English)), *, *)
        .returns(Future successful PreferencesExists)

      await(service.paperlessSettingsOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English)))) shouldBe PreferencesExists
    }

    "If sent, override the accepted value to 'false' when opting out" in {
      mockAudit(transactionName = "paperlessSettingsOptOut")
      (entityResolver
        .paperlessOptOut(_: PaperlessOptOut)(_: HeaderCarrier, _: ExecutionContext))
        .expects(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English)), *, *)
        .returns(Future successful PreferencesExists)

      await(service.paperlessSettingsOptOut(PaperlessOptOut(Some(TermsAccepted(Some(true))), Some(English)))) shouldBe PreferencesExists
    }
  }

  "reOptInEnabledCheck" should {
    "pass through ReOptIn status if enabled" in {
      val expectedPreferences = preferencesWithStatus(ReOptIn)

      service.reOptInEnabledCheck(expectedPreferences) shouldBe
      expectedPreferences.copy(
        status = Some(
          PaperlessStatus(ReOptIn, category = Category.Info)
        )
      )
    }
    "replace ReOptIn status with Verified if disabled" in {
      val service =
        new CustomerProfileService(
          citizenDetailsConnector,
          preferencesConnector,
          entityResolver,
          accountAccessControl,
          auditConnector,
          "customer-profile",
          false,
          auditService
        )
      val expectedPreferences = preferencesWithStatus(ReOptIn)

      service.reOptInEnabledCheck(expectedPreferences) shouldBe
      expectedPreferences.copy(
        status = Some(
          PaperlessStatus(Verified, category = Category.Info)
        )
      )
    }
  }

}
