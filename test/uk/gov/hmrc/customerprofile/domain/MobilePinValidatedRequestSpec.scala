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
import play.api.libs.json._

class MobilePinValidatedRequestSpec extends AnyWordSpec with Matchers {

  "MobilePinValidatedRequest" should {

    "serialize to JSON correctly" in {
      val request = MobilePinValidatedRequest("1234", "device-xyz")
      val json = Json.toJson(request)

      json shouldBe Json.parse("""
        {
          "pin": "1234",
          "deviceId": "device-xyz"
        }
      """)
    }

    "deserialize from valid JSON correctly" in {
      val json = Json.parse("""
        {
          "pin": "4321",
          "deviceId": "device-abc"
        }
      """)

      val result = json.validate[MobilePinValidatedRequest]

      result.isSuccess shouldBe true
      result.get shouldBe MobilePinValidatedRequest("4321", "device-abc")
    }

    "fail to deserialize from JSON with missing fields" in {
      val json = Json.parse("""
        {
          "pin": "1111"
        }
      """)

      val result = json.validate[MobilePinValidatedRequest]

      result.isError shouldBe true
    }

    "fail to deserialize from JSON with wrong field types" in {
      val json = Json.parse("""
        {
          "pin": 1234,
          "deviceId": true
        }
      """)

      val result = json.validate[MobilePinValidatedRequest]

      result.isError shouldBe true
    }
  }
}
