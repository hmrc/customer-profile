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

import java.time.LocalDate
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{prettyPrint, toJson}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

class DomainFormatCheckSpec extends BaseSpec {

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

  val nino:                               Nino            = new Generator().nextNino
  val email:                              EmailAddress    = EmailAddress("name@email.co.uk")
  val paperless:                          Paperless       = Paperless(TermsAccepted(Some(true)), email, Some(English))
  val verifiedEmailPreference:            Preference      = Preference(digital = true)
  val paperlessOptOut:                    PaperlessOptOut = PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))
  lazy val paperlessAsJson:               JsValue         = toJson(paperless)
  lazy val paperlessOptOutAsJson:         JsValue         = toJson(paperlessOptOut)
  lazy val verifiedEmailPreferenceAsJson: JsValue         = toJson(verifiedEmailPreference)

  val person =
    Person(
      Some("John"),
      Some("Albert"),
      Some("Smith"),
      None,
      Some("Mr"),
      None,
      Some("M"),
      None,
      Some(LocalDate.now.minusYears(30)),
      Some(nino),
      Some("John Albert"),
      Some("/personal-account/national-insurance-summary/save-letter-as-pdf")
    )
  val address:                    Option[Address]   = None
  val personalDetails:            PersonDetails     = PersonDetails(person, address, None)
  val applePass:                  RetrieveApplePass = RetrieveApplePass("TXIgSm9lIEJsb2dncw==")
  lazy val personalDetailsAsJson: JsValue           = toJson(personalDetails)
  lazy val applePassJson:         JsValue           = toJson(applePass)

}
