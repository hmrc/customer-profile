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
import uk.gov.hmrc.customerprofile.connector.{ApplePassConnector, CitizenDetailsConnector}
import uk.gov.hmrc.customerprofile.domain.{Person, PersonDetails, RetrieveApplePass}
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.domain.Nino

import scala.concurrent.{ExecutionContext, Future}

class ApplePassServiceSpec extends BaseSpec {

  val appNameConfiguration: Configuration  = mock[Configuration]
  val auditConnector:       AuditConnector = mock[AuditConnector]
  val passId:               String         = "c864139e-77b5-448f-b443-17c69060870d"
  val base64String:         String         = "TXIgSm9lIEJsb2dncw=="

  val person: PersonDetails = PersonDetails(
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

  def mockGetAccounts() = {
    mockAudit(transactionName = "getApplePass")
    (accountAccessControl
      .retrieveNino()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful Some(nino))
  }

  def mockCreateApplePass(f: Future[String]) =
    (getApplePassConnector
      .createApplePass(_: String, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(nino.formatted, *, *, *)
      .returning(f)

  def mockGetApplePass(f: Future[RetrieveApplePass]) =
    (getApplePassConnector
      .getApplePass(_: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(passId, *, *)
      .returning(f)

  val citizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val getApplePassConnector:   ApplePassConnector      = mock[ApplePassConnector]
  val accountAccessControl:    AuthRetrievals          = mock[AuthRetrievals]
  val auditService: AuditService = new AuditService(auditConnector, "customer-profile")

  val service = new ApplePassService(
    citizenDetailsConnector,
    getApplePassConnector,
    accountAccessControl,
    auditConnector,
    "customer-profile",
    auditService
  )

  "getApplePass" should {
    "audit and return an apple pass model with a base 64 encoded string" in {

      mockGetAccounts()
      mockAudit(transactionName = "applePass")

      (citizenDetailsConnector
        .personDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *)
        .returns(Future successful person)

      mockCreateApplePass(Future.successful(passId))
      mockGetApplePass(Future.successful(RetrieveApplePass(base64String)))

      val result = await(service.getApplePass())
      result shouldBe RetrieveApplePass(base64String)
    }
  }
}
