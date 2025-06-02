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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

class MobilePinSpec extends AnyWordSpec with Matchers {

  val uuid     = UUID.randomUUID().toString
  val dateTime = LocalDateTime.of(2025, 4, 4, 0, 0) // 4 April 2025 at 00:00
  val instant  = dateTime.toInstant(ZoneOffset.UTC)

  val mobilePin  = MobilePin(uuid, List("1234", "5678", "9012"), Some(instant), Some(instant))
  val mobileJson = Json.parse(s"""{
                                 |  "deviceId": "$uuid",
                                 |  "hashedPins": [
                                 |    "1234",
                                 |    "5678",
                                 |    "9012"
                                 |  ],
                                 |  "createdAt": "2025-04-04T00:00:00Z",
                                 |  "updatedAt": "2025-04-04T00:00:00Z"
                                 |}
                                 |""".stripMargin)

  "perform JOSN de/serialisation correctly" should {
    "serialize mobile pin data" in {
      Json.toJson(mobilePin) shouldBe mobileJson
    }

    "convert instant to Local date time" in {
      val result = MobilePin.convertInstantToLocalTime(instant)
      result shouldBe (LocalDateTime.of(2025, 4, 4, 1, 0))
    }
  }

}
