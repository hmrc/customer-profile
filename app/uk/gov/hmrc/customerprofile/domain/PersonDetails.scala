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

import java.time.{LocalDate, ZoneOffset}
import play.api.libs.json._
import play.api.libs.json.Reads.DefaultLocalDateReads
import uk.gov.hmrc.domain.Nino
import play.api.libs.functional.syntax._

object Person {
  implicit val writes: Writes[Person] = Json.writes[Person]

  implicit val reads: Reads[Person] = (
    (JsPath \ "firstName").readNullable[String] and
    (JsPath \ "middleName").readNullable[String] and
    (JsPath \ "lastName").readNullable[String] and
    (JsPath \ "initials").readNullable[String] and
    (JsPath \ "title").readNullable[String] and
    (JsPath \ "honours").readNullable[String] and
    (JsPath \ "sex").readNullable[String] and
    (JsPath \ "dateOfBirth").readNullable[LocalDate] and
    (JsPath \ "nino").readNullable[Nino] and
    (JsPath \ "fullName").readNullable[String] and
    (JsPath \ "nationalInsuranceLetterUrl").readNullable[String]
  )(Person.apply)
}

case class Person(
  firstName:                  Option[String],
  middleName:                 Option[String],
  lastName:                   Option[String],
  initials:                   Option[String],
  title:                      Option[String],
  honours:                    Option[String],
  sex:                        Option[String],
  personDateOfBirth:          Option[LocalDate],
  nino:                       Option[Nino],
  fullName:                   Option[String],
  nationalInsuranceLetterUrl: Option[String]) {

  lazy val shortName: String =
    List(firstName, middleName, lastName).flatten.mkString(" ")

  lazy val completeName: String =
    List(title, firstName, middleName, lastName, honours).flatten.mkString(" ")
}

object Address {
  implicit val formats: OFormat[Address] = Json.format[Address]
}

case class Address(
  line1:             Option[String] = None,
  line2:             Option[String] = None,
  line3:             Option[String] = None,
  line4:             Option[String] = None,
  line5:             Option[String] = None,
  postcode:          Option[String] = None,
  country:           Option[String] = None,
  startDate:         Option[LocalDate] = None,
  `type`:            Option[String] = None,
  changeAddressLink: Option[String]) {
  startDate.map(_.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli)
}

object PersonDetails {
  implicit val formats: OFormat[PersonDetails] = Json.format[PersonDetails]
}

case class PersonDetails(
  person:                Person,
  address:               Option[Address],
  correspondenceAddress: Option[Address])
