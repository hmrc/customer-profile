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

import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import org.scalamock.handlers.CallHandler3
import org.scalamock.matchers.MatcherBase
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, GooglePassConnector}
import uk.gov.hmrc.customerprofile.domain.{Person, PersonDetails, RetrieveGooglePass}
import uk.gov.hmrc.customerprofile.utils.{BaseSpec, GoogleCredentialsHelper}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}

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
  val auditService: AuditService = new AuditService(auditConnector, "customer-profile")

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
    mockAudit(transactionName = "getGooglePass")
    (accountAccessControl
      .retrieveNino()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful Some(nino))
  }

  def mockCreateGooglePass(f: Future[String]) =
    (getGooglePassConnector
      .createGooglePassWithCredentials(_: String, _: String, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(*, nino.formatted, *, *, *)
      .returning(f)

  def mockGoogleCredentialsHelper(f: String) =
    (googleCredentialsHelper
      .createGoogleCredentials(_: String))
      .expects(googleKey)
      .returning(f)

  def mockGetGooglePass(f: Future[RetrieveGooglePass]) =
    (getGooglePassConnector
      .getGooglePassUrl(_: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(passId, *, *)
      .returning(f)

  val service = new GooglePassService(
    citizenDetailsConnector,
    getGooglePassConnector,
    accountAccessControl,
    googleCredentialsHelper,
    auditConnector,
    "customer-profile",
    googleKey,
    auditService
  )

}
