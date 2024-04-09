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

package uk.gov.hmrc.customerprofile.utils

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.mocks.{AuthorisationMock, ShutteringMock}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import eu.timepit.refined.auto._
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.customerprofile.connector.ShutteringConnector
import uk.gov.hmrc.customerprofile.domain.Shuttering

import scala.concurrent.ExecutionContext

trait BaseSpec
    extends AnyWordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockFactory
    with ShutteringMock
    with AuthorisationMock {

  implicit lazy val hc:                 HeaderCarrier       = HeaderCarrier()
  implicit lazy val ec:                 ExecutionContext    = scala.concurrent.ExecutionContext.Implicits.global
  implicit val shutteringConnectorMock: ShutteringConnector = mock[ShutteringConnector]

  val appName:              String      = "customer-profile"
  val nino:                 Nino        = Nino("CS700100A")
  val journeyId:            JourneyId   = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val acceptheader:         String      = "application/vnd.hmrc.1.0+json"
  val grantAccessWithCL200: GrantAccess = Some(nino.nino) and L200

  val shuttered: Shuttering =
    Shuttering(
      shuttered = true,
      Some("Shuttered"),
      Some("Customer-Profile is currently not available")
    )
  val notShuttered: Shuttering = Shuttering.shutteringDisabled

  val requestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Accept" -> acceptheader)
  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val requestWithoutAcceptHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Authorization" -> "Some Header")

  val invalidPostRequest: FakeRequest[JsValue] =
    FakeRequest()
      .withBody(Json.parse("""{ "blah" : "blah" }"""))
      .withHeaders(HeaderNames.ACCEPT -> acceptheader)

}
