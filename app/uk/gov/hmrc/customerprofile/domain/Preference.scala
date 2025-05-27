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

import play.api.libs.json.*
import uk.gov.hmrc.customerprofile.emailaddress.EmailAddress

import java.time.LocalDate

case class EmailPreference(
  email:    EmailAddress,
  status:   StatusName,
  linkSent: Option[LocalDate] = None)

object EmailPreference {

  implicit val localdateFormatDefault: Format[LocalDate] =
    Format(Reads.DefaultLocalDateReads, Writes.DefaultLocalDateWrites)

  implicit val formats: OFormat[EmailPreference] = Json.format[EmailPreference]
}

case class Preference(
  digital:      Boolean,
  emailAddress: Option[String] = None,
  linkSent:     Option[LocalDate] = None,
  email:        Option[EmailPreference] = None,
  status:       Option[PaperlessStatus] = None)

object Preference {

  implicit val localdateFormatDefault: Format[LocalDate] =
    Format(Reads.DefaultLocalDateReads, Writes.DefaultLocalDateWrites)

  implicit val format: OFormat[Preference] = {
    Json.format[Preference]
  }
}
