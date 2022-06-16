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

import eu.timepit.refined.auto._
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L50}
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.stubs.AuthStub._
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.http.HeaderNames._

class AccountAccessControlISpec extends BaseISpec with Eventually {

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer 123")))

  val saUtr:                    SaUtr                = SaUtr("1872796160")
  val nino:                     Nino                 = Nino("CS100700A")
  val journeyId:                JourneyId            = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val testAccountAccessControl: AccountAccessControl = app.injector.instanceOf[AccountAccessControl]

  "Returning the nino" should {
    "be found and routeToIV and routeToTwoFactor should be true" in {
      ninoFound(nino)

      val foundNino: Option[Nino] = await(testAccountAccessControl.retrieveNino()(hc))
      foundNino.get shouldBe nino
    }

  }

  "grantAccess" should {
    "error with unauthorised when account has low CL" in {
      authRecordExists(nino, L50)

      intercept[AccountWithLowCL] {
        await(testAccountAccessControl.grantAccess(Some(nino)))
      }
    }

    "find NINO only account when cCL is correct" in {
      authRecordExists(nino, L200)
      await(testAccountAccessControl.grantAccess(Some(nino)))
    }

    "fail to return authority when no NINO exists" in {
      authRecordExistsWithoutNino

      intercept[NinoNotFoundOnAccount] {
        await(testAccountAccessControl.grantAccess(Some(nino)))
      }
    }

    "fail to return authority when auth NINO does not match request NINO" in {
      authRecordExists(nino, L200)

      intercept[FailToMatchTaxIdOnAuth] {
        await(testAccountAccessControl.grantAccess(Some(Nino("CS333700A"))))
      }
    }
  }
}
