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

package uk.gov.hmrc.customerprofile.domain

import play.api.Logger
import play.api.libs.json.{Format, JsError, JsResult, JsSuccess, JsValue, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDateTime, ZoneId}

case class MobilePin(deviceId: String,
                     ninoHash: String,
                     hashedPins: List[String],
                     createdAt: Option[Instant] = None,
                     updatedAt: Option[Instant] = None
                    )

object MobilePin {

  implicit val instantReads: Reads[Instant] = stringOrDate => {
    MongoJavatimeFormats.instantFormat.reads(stringOrDate) match {
      case s @ JsSuccess(_, _) => s
      case _ @JsError(_)       => handleStringDate(stringOrDate)
    }
  }

  implicit val instantWrites: Writes[Instant] = MongoJavatimeFormats.instantWrites

  // $COVERAGE-OFF$
  private def handleStringDate(stringDate: JsValue): JsResult[Instant] = {
    Logger(this.getClass).warn("Failed trying to read mongo date format. Next trying to read string date format")
    Json.fromJson[Instant](stringDate)(play.api.libs.json.Reads.DefaultInstantReads) match {
      case s @ JsSuccess(_, _) => s
      case f @ JsError(_) =>
        Logger(this.getClass)
          .warn("Failed to read a date from mongo date format and string format")
        f
    }
  }
  implicit val format: Format[MobilePin] = Json.format[MobilePin]

  def convertInstantToLocalTime(instant: Instant): LocalDateTime =
    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London"))
}
