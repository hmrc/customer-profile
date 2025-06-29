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

import com.google.inject.{Inject, Singleton}

import javax.inject.Named
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.*
import uk.gov.hmrc.customerprofile.controllers.NinoNotFoundOnAccount
import uk.gov.hmrc.customerprofile.domain.Category.Info
import uk.gov.hmrc.customerprofile.domain.StatusName.{ReOptIn, Verified}
import uk.gov.hmrc.customerprofile.domain.*
import uk.gov.hmrc.customerprofile.domain.types.JourneyId
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerProfileService @Inject() (citizenDetailsConnector: CitizenDetailsConnector,
                                        preferencesConnector: PreferencesConnector,
                                        entityResolver: EntityResolverConnector,
                                        val accountAccessControl: AuthRetrievals,
                                        @Named("appName") val appName: String,
                                        @Named("reOptInEnabled") val reOptInEnabled: Boolean,
                                        auditService: AuditService
                                       ) {

  def getNino(
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Nino]] =
    auditService.withAudit("getAccounts", Map.empty) {
      accountAccessControl.retrieveNino()
    }

  def getPersonalDetails(
    nino: Nino
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] =
    auditService.withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector
        .personDetails(nino)
        .map(details =>
          details.copy(
            person = details.person.copy(
              fullName                   = Some(details.person.shortName),
              nationalInsuranceLetterUrl = Some("/save-your-national-insurance-number/print-letter/save-letter-as-pdf")
            ),
            address = Some(
              details.address
                .getOrElse(Address(changeAddressLink = Some("/personal-account/profile-and-settings")))
                .copy(changeAddressLink = Some("/personal-account/profile-and-settings"))
            )
          )
        )
    }

  def paperlessSettings(
    settings: Paperless,
    journeyId: JourneyId
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    auditService.withAudit("paperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      for {
        preferences <- entityResolver.getPreferences()
        status <- preferences.fold(paperlessOptIn(settings)) { preference =>
                    if (
                      preference.digital && preference.status
                        .getOrElse(PaperlessStatus(Verified, Info))
                        .name != ReOptIn
                    ) setPreferencesPendingEmail(ChangeEmail(settings.email.value))
                    else paperlessOptIn(settings)
                  }
      } yield status
    }

  def paperlessSettingsOptOut(
    paperlessOptOut: PaperlessOptOut
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    auditService.withAudit("paperlessSettingsOptOut", Map.empty) {
      val genericOptOut = paperlessOptOut.generic.getOrElse(TermsAccepted(Some(false))).copy(accepted = Some(false))
      entityResolver.paperlessOptOut(
        paperlessOptOut.copy(generic = Some(genericOptOut))
      )
    }

  def getPreferences(
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] =
    auditService.withAudit("getPreferences", Map.empty) {
      copyToResponsePayload(entityResolver.getPreferences())
    }

  def setPreferencesPendingEmail(
    changeEmail: ChangeEmail
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    auditService.withAudit("updatePendingEmailPreference", Map("email" -> changeEmail.email)) {
      for {
        nino     <- getNino()
        entity   <- entityResolver.getEntityIdByNino(nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        response <- preferencesConnector.updatePendingEmail(changeEmail, entity._id)
      } yield response
    }

  def reOptInEnabledCheck(preferences: Preference): Preference =
    (reOptInEnabled, preferences.status.map(_.name)) match {
      case (false, Some(ReOptIn)) =>
        preferences.copy(status = preferences.status.map(_.copy(name = Verified)))
      case _ => preferences
    }

  private def copyToResponsePayload(
    preferencesReceived: Future[Option[Preference]]
  )(implicit ex: ExecutionContext): Future[Option[Preference]] =
    for {
      emailAddressCopied <- preferencesReceived.map(
                              _.map(pref => pref.copy(emailAddress = pref.email.map(_.email.value)))
                            )
      linkSent <- preferencesReceived.map(_.flatMap(_.email.flatMap(_.linkSent)))
      backwardsCompatiblePreferences <- if (linkSent.isDefined)
                                          Future successful emailAddressCopied.map(pref => pref.copy(linkSent = linkSent, email = None))
                                        else Future successful emailAddressCopied.map(pref => pref.copy(email = None))
    } yield backwardsCompatiblePreferences

  private def paperlessOptIn(
    settings: Paperless
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] = entityResolver.paperlessSettings(
    settings.copy(generic = settings.generic.copy(accepted = Some(true)))
  )

}
