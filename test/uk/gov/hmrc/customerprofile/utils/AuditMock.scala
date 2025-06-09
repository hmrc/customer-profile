/*
 * Copyright 2025 HM Revenue & Customs
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

//package uk.gov.hmrc.customerprofile.utils
//
//import org.scalamock.matchers.MatcherBase
//import org.scalamock.scalatest.MockFactory
//import uk.gov.hmrc.customerprofile.domain.audit.MobilePinAudit
//import uk.gov.hmrc.domain.Nino
//import uk.gov.hmrc.http.HeaderCarrier
//import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
//import uk.gov.hmrc.play.audit.model.DataEvent
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.Success
//
//trait AuditMock extends MockFactory{
//
//  def dataEventWith(
//    auditSource : String,
//    auditType   : String,
//    tags        : String,
//    details     : Map[String, String]
//                   ): MatcherBase =
//    argThat{( dataEvent: DataEvent) =>
//      dataEvent.auditSource.equals(auditSource) &&
//        dataEvent.auditType.equals(auditType) &&
//        dataEvent.tags("tags").equals(tags) &&
//        dataEvent.tags.get("path").isDefined &&
//        dataEvent.tags.get("clientIP").isDefined &&
//        dataEvent.tags.get("clientPort").isDefined &&
//        dataEvent.tags.get("X-Request-ID").isDefined &&
//        dataEvent.tags.get("X-Session-ID").isDefined &&
//        dataEvent.tags.get("Unexpected").isEmpty &&
//        dataEvent.detail.equals(details)}
//
//
//  def mockAudit(
//    nino: Option[Nino],
//    expectedResponse: MobilePinAudit,
//    path: String,
//    journeyId: String
//               )(implicit auditConnector: AuditConnector): Unit ={
//    (auditConnector.sendEvent(_: DataEvent)(_ :HeaderCarrier, _: ExecutionContext))
//      .expects(dataEventWith("mobile-pin-security",
//      "mobile-pin-security",
//        "Pin-Updated-Inserted",
//        Map("nino" -> Future.successful(Some(nino)).toString, "data" -> expectedResponse.toString))
//        *,
//        *)
//      .returning(Future successful Success)
//  }
//
//}
