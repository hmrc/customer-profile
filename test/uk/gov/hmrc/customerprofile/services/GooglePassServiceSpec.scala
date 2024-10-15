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
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, GooglePassConnector}
import uk.gov.hmrc.customerprofile.domain.RetrieveGooglePass
import uk.gov.hmrc.customerprofile.utils.{BaseSpec, GoogleCredentialsHelper}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class GooglePassServiceSpec extends BaseSpec {

  val appNameConfiguration:    Configuration           = mock[Configuration]
  val auditConnector:          AuditConnector          = mock[AuditConnector]
  val getGooglePassConnector:  GooglePassConnector     = mock[GooglePassConnector]
  val citizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val accountAccessControl:    AuthRetrievals          = mock[AuthRetrievals]
  val googleCredentialsHelper: GoogleCredentialsHelper = mock[GoogleCredentialsHelper]
  val passId:                  String                  = "c864139e-77b5-448f-b443-17c69060870d"
  val jwtUrl:                  String                  = "www.url.com/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
  val googleKey:               String                  = "123456789"
  val auditService:            AuditService            = mock[AuditService]
  implicit val defaultTimeout: FiniteDuration          = 5 seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val googlePassService = new GooglePassService(
    citizenDetailsConnector,
    getGooglePassConnector,
    accountAccessControl,
    googleCredentialsHelper,
    "customer-profile",
    googleKey,
    auditService
  )
  "GOOGLE pass" should {
    "audit and return a  google pass model with a base 64 encoded string" in {
      when(accountAccessControl.retrieveNino()(any(), any())).thenReturn(Future.successful(Some(nino)))
      when(auditService.withAudit[RetrieveGooglePass](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(RetrieveGooglePass(googleKey)))
      when(citizenDetailsConnector.personDetails(any())(any(), any())).thenReturn(Future.successful(person2))
      when(getGooglePassConnector.createGooglePassWithCredentials(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(passId))
      when(getGooglePassConnector.getGooglePassUrl(any())(any(), any()))
        .thenReturn(Future.successful(RetrieveGooglePass(googleKey)))
      val result = await(googlePassService.getGooglePass())
      result mustBe RetrieveGooglePass(googleKey)
    }

    "audit and return a  google QR Code in Bytes" in {

      val qrCode = "QRcode".getBytes()
      when(accountAccessControl.retrieveNino()(any(), any())).thenReturn(Future.successful(Some(nino)))
      when(auditService.withAudit[Option[Array[Byte]]](any(), any())(any())(any(), any()))
        .thenReturn(Future.successful(Some(qrCode)))
      when(citizenDetailsConnector.personDetails(any())(any(), any())).thenReturn(Future.successful(person2))
      when(getGooglePassConnector.createGooglePassWithCredentials(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(passId))
      when(getGooglePassConnector.getGoogleQRCode(any())(any(), any()))
        .thenReturn(Future.successful(Some(qrCode)))
      val result = await(googlePassService.getGoogleQRCode())
      result mustBe Some(qrCode)
    }

  }

}
