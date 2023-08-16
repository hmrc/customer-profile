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
import com.google.inject.name.Named
import uk.gov.hmrc.customerprofile.auth.AccountAccessControl
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, GooglePassConnector}
import uk.gov.hmrc.customerprofile.controllers.NinoNotFoundOnAccount
import uk.gov.hmrc.customerprofile.domain.RetrieveGooglePass
import uk.gov.hmrc.customerprofile.utils.GoogleCredentialsHelper
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

class GooglePassService @Inject()( citizenDetailsConnector: CitizenDetailsConnector,
                                   googlePassConnector: GooglePassConnector,
                                   accountAccessControl: AccountAccessControl,
                                   googleCredentialsHelper: GoogleCredentialsHelper,
                                  val auditConnector: AuditConnector,
                                  @Named("appName") val appName: String,
                                  @Named("key") val key: String ) extends Auditor{

  def getNino(
             )(implicit hc: HeaderCarrier,
               ex: ExecutionContext
             ): Future[Option[Nino]] = {
    withAudit("getApplePass", Map.empty) {
      accountAccessControl.retrieveNino()
    }
  }

  def retrieveJwt(url: String): String = {
    url.stripPrefix("https://pay.google.com/gp/v/save/")
  }

  def getGooglePass()(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[RetrieveGooglePass] = {
    withAudit("googlePass", Map.empty) {
      for {
        nino <- getNino()
        citizenDetails <- citizenDetailsConnector.personDetails(nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        getGooglePass <- googlePassConnector.createGooglePassWithCredentials(citizenDetails.person.completeName, nino.get.formatted, googleCredentialsHelper.createGoogleCredentials(key))
        retrievePass <- googlePassConnector.getGooglePassUrl(getGooglePass)
        retrieveGooglePass: RetrieveGooglePass = RetrieveGooglePass(retrieveJwt(retrievePass.googlePass))
      } yield retrieveGooglePass
    }
  }




}
