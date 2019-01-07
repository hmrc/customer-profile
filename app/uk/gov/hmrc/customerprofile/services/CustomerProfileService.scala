/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.customerprofile.auth.{AccountAccessControl, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerProfileService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
                                       preferencesConnector: PreferencesConnector,
                                       entityResolver: EntityResolverConnector,
                                       val accountAccessControl: AccountAccessControl,
                                       val appNameConfiguration: Configuration,
                                       val auditConnector: AuditConnector) extends Auditor {
  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts] =
    withAudit("getAccounts", Map.empty) {
      accountAccessControl.accounts
    }

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] =
    withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector.personDetails(nino)
    }

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("paperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      for {
        preferences ← entityResolver.getPreferences()
        status ← preferences.fold(entityResolver.paperlessSettings(settings)) {
          preference =>
            if (preference.digital) setPreferencesPendingEmail(ChangeEmail(settings.email.value))
            else entityResolver.paperlessSettings(settings)
        }
      } yield status
    }

  def paperlessSettingsOptOut()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("paperlessSettingsOptOut", Map.empty) {
      entityResolver.paperlessOptOut()
    }

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] =
    withAudit("getPreferences", Map.empty) {
      entityResolver.getPreferences()
    }

  private def setPreferencesPendingEmail(changeEmail: ChangeEmail)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("updatePendingEmailPreference", Map("email" → changeEmail.email)) {
      for {
        account ← getAccounts()
        entity ← entityResolver.getEntityIdByNino(account.nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        response ← preferencesConnector.updatePendingEmail(changeEmail, entity._id)
      } yield response
    }
  
}
