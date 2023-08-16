package uk.gov.hmrc.customerprofile.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import eu.timepit.refined.auto._
import uk.gov.hmrc.customerprofile.domain.{Person, PersonDetails}
import uk.gov.hmrc.domain.Nino

class GooglePassServiceSpec
  extends AnyWordSpecLike
  with Matchers
  with FutureAwaits
  with DefaultAwaitTimeout
  with MockFactory {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val appNameConfiguration: Configuration = mock[Configuration]
  val auditConnector: AuditConnector = mock[AuditConnector]
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val appName: String = "customer-profile"
  val passId = "c864139e-77b5-448f-b443-17c69060870d"
  val jwtString: String = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
  val nino: Nino = Nino("CS700100A")

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
    mockAudit(transactionName = "getApplePass")
    (accountAccessControl
      .retrieveNino()(_: HeaderCarrier))
      .expects(*)
      .returns(Future successful Some(nino))
  }

}
