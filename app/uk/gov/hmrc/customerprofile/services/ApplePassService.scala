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

import com.google.inject.Inject
import uk.gov.hmrc.customerprofile.auth.AccountAccessControl
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, ApplePassConnector}
import uk.gov.hmrc.customerprofile.controllers.NinoNotFoundOnAccount
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class ApplePassService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
                                 createApplePassConnector: ApplePassConnector,
                                 accountAccessControl: AccountAccessControl,
                                 val auditConnector: AuditConnector,
                                 @Named("appName") val appName: String) extends Auditor {
  def getNino(
             )(implicit hc: HeaderCarrier,
               ex: ExecutionContext
             ): Future[Option[Nino]] =
    withAudit("getApplePass", Map.empty) {
      accountAccessControl.retrieveNino()
    }

  def getApplePass()(implicit hc: HeaderCarrier, executionContext: ExecutionContext) : Future[RetrieveApplePass] = {
    withAudit("applePass", Map.empty) {
      for {
        nino <- getNino()
        citizenDetails <- citizenDetailsConnector.personDetails(nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        getApplePassUUID <- createApplePassConnector.createApplePass(nino.getOrElse(throw new NinoNotFoundOnAccount("")), citizenDetails.person.completeName)
        getApplePassGenerator <- createApplePassConnector.getPass(getApplePassUUID.uuid)
        applePass: RetrieveApplePass = RetrieveApplePass(getApplePassGenerator.applePass)
      } yield applePass
    }
  }


}
