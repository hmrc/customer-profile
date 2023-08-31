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

package uk.gov.hmrc.customerprofile.domain

import org.scalatest.matchers.should.Matchers

import java.time.LocalDate
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{prettyPrint, toJson}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.util.Random

class DomainFormatCheckSpec extends AnyWordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout {

  import DomainGenerator._

  val logger: Logger = Logger(this.getClass)

  "Personal details" in {
    logger.debug(
      "Personal details response : " + prettyPrint(personalDetailsAsJson)
    )
  }

  "Paperless" in {
    logger.debug("Paperless request : " + prettyPrint(paperlessAsJson))
  }

  "Paperless opt out" in {
    logger.debug(
      "Paperless opt out response : " + prettyPrint(paperlessOptOutAsJson)
    )
  }

  "Verified email Preference" in {
    logger.debug(
      "Preference response : " + prettyPrint(verifiedEmailPreferenceAsJson)
    )
  }

  "Apple pass" in {
    logger.debug(
      "Apple pass response : " + prettyPrint(applePassJson)
    )
  }
}

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val nino: Nino = new Generator().nextNino

  val email = EmailAddress("name@email.co.uk")

  val paperless = Paperless(TermsAccepted(Some(true)), email, Some(English))
  lazy val paperlessAsJson: JsValue = toJson(paperless)

  val paperlessOptOut = PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))
  lazy val paperlessOptOutAsJson: JsValue = toJson(paperlessOptOut)

  val verifiedEmailPreference =
    Preference(digital = true)

  lazy val verifiedEmailPreferenceAsJson: JsValue = toJson(
    verifiedEmailPreference
  )

  val person =
    Person(
      Some("John"),
      Some("Albert"),
      Some("Smith"),
      None,
      Some("Mr"),
      None,
      Some("M"),
      Some(LocalDate.now.minusYears(30)),
      Some(nino),
      Some("John Albert"),
      Some("/personal-account/national-insurance-summary/save-letter-as-pdf")
    )
  val address: Option[Address] = None
  val personalDetails = PersonDetails(person, address, None)
  lazy val personalDetailsAsJson: JsValue           = toJson(personalDetails)
  val applePass:                  RetrieveApplePass = RetrieveApplePass("TXIgSm9lIEJsb2dncw==")
  lazy val applePassJson:         JsValue           = toJson(applePass)

}

//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new Random(seed))

  def randomNext: Int = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
