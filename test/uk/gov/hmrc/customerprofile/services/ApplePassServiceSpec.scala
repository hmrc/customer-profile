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
import org.mockito.Mockito.when
import play.api.Configuration
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.{ApplePassConnector, CitizenDetailsConnector}
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class ApplePassServiceSpec extends BaseSpec {

  val appNameConfiguration: Configuration = mock[Configuration]
  val passId: String = "c864139e-77b5-448f-b443-17c69060870d"
  val base64String: String = "TXIgSm9lIEJsb2dncw=="
  implicit val defaultTimeout: FiniteDuration = 5 seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val citizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val getApplePassConnector: ApplePassConnector = mock[ApplePassConnector]
  val accountAccessControl: AuthRetrievals = mock[AuthRetrievals]
  val auditService: AuditService = mock[AuditService]

  val service = new ApplePassService(
    citizenDetailsConnector,
    getApplePassConnector,
    accountAccessControl,
    "customer-profile",
    auditService
  )

  "getApplePass" should {
    "audit and return an apple pass model with a base 64 encoded string" in {

      when(accountAccessControl.retrieveNino()(any(), any())).thenReturn(Future.successful(Some(nino)))
      when(auditService.withAudit[RetrieveApplePass](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(RetrieveApplePass(base64String)))
      when(citizenDetailsConnector.personDetails(any())(any(), any())).thenReturn(Future.successful(person2))
      when(getApplePassConnector.createApplePass(any(), any())(any(), any())).thenReturn(Future.successful(passId))
      when(getApplePassConnector.getApplePass(any())(any(), any()))
        .thenReturn(Future.successful(RetrieveApplePass(base64String)))

      val result = await(service.getApplePass())
      result mustBe RetrieveApplePass(base64String)
    }
  }
}
