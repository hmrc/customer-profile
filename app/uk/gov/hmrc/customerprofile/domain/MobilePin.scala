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

import org.bson.codecs.pojo.annotations.BsonId
import org.mongodb.scala.bson.annotations.BsonProperty
import play.api.libs.json.{Format, Json}

import java.time.{Instant, LocalDateTime, ZoneId}

case class MobilePin(
  @BsonId
  @BsonProperty("_id")
  deviceId:   String,
  ninoHash:   String,
  hashedPins: List[String],
  createdAt:  Option[Instant] = None,
  updatedAt:  Option[Instant] = None)

object MobilePin {
  implicit val format: Format[MobilePin] = Json.format[MobilePin]

  def convertInstantToLocalTime(instant: Instant): LocalDateTime =
    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London"))
}
