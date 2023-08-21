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

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import eu.timepit.refined.auto._
import org.scalamock.handlers.CallHandler3
import org.scalamock.matchers.MatcherBase
import uk.gov.hmrc.customerprofile.auth.AccountAccessControl
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, GooglePassConnector}
import uk.gov.hmrc.customerprofile.domain.{Person, PersonDetails, RetrieveGooglePass}
import uk.gov.hmrc.customerprofile.utils.GoogleCredentialsHelper
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class GooglePassServiceSpec
  extends AnyWordSpecLike
  with Matchers
  with FutureAwaits
  with DefaultAwaitTimeout
  with MockFactory {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val appNameConfiguration: Configuration = mock[Configuration]
  val auditConnector: AuditConnector = mock[AuditConnector]
  val getGooglePassConnector: GooglePassConnector = mock[GooglePassConnector]
  val citizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val appName: String = "customer-profile"
  val passId = "c864139e-77b5-448f-b443-17c69060870d"
  val jwtUrl: String = "www.url.com/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
  val googleKey: String = "123456789"
  val nino: Nino = Nino("CS700100A")
  val accountAccessControl: AccountAccessControl = mock[AccountAccessControl]
  val googleCredentialsHelper: GoogleCredentialsHelper = mock[GoogleCredentialsHelper]

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
    None
  )

  def mockAudit(
                 transactionName: String,
                 detail: Map[String, String] = Map.empty
               ): CallHandler3[DataEvent, HeaderCarrier, ExecutionContext, Future[
    AuditResult
  ]] = {
    def dataEventWith(
                       auditSource: String,
                       auditType: String,
                       tags: Map[String, String]
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
          tags = Map("transactionName" -> transactionName)
        ),
        *,
        *
      )
      .returns(Future successful Success)
  }

  def mockGetAccounts() = {
    mockAudit(transactionName = "getGooglePass")
    (accountAccessControl
      .retrieveNino()(_: HeaderCarrier))
      .expects(*)
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
      .expects(passId,*,*)
      .returning(f)

  val service = new GooglePassService(
    citizenDetailsConnector,
    getGooglePassConnector,
    accountAccessControl,
    googleCredentialsHelper,
    auditConnector,
    "customer-profile",
    googleKey
  )


}
