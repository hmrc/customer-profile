/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.auth

import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.customerprofile.stubs.AuthStub.ninoFound
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.ExecutionContext

class AccountAccessControlISpec extends BaseISpec with Eventually {

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer 123")))
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val testAuthRetrievals: AuthRetrievals = app.injector.instanceOf[AuthRetrievals]

  "Returning the nino" should {
    "be found and routeToIV and routeToTwoFactor should be true" in {
      ninoFound(nino)

      val foundNino: Option[Nino] = await(testAuthRetrievals.retrieveNino()(hc, ec))
      foundNino.get shouldBe nino
    }

  }
}
