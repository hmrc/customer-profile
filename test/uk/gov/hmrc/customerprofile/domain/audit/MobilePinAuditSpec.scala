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

package uk.gov.hmrc.customerprofile.domain.audit

import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.MobilePinValidatedRequest
import uk.gov.hmrc.customerprofile.utils.BaseSpec

class MobilePinAuditSpec extends BaseSpec {
  "MobilePinAudit.fromResponse" should {

    "correctly create MobilePinAudit from MobilePinValidatedRequest" in {
      val response = MobilePinValidatedRequest(pin = "123456", deviceId = "device-abc")
      val audit = MobilePinAudit.fromResponse(response)

      audit.pin      shouldBe "123456"
      audit.deviceId shouldBe "device-abc"
    }
  }

  "MobilePinAudit JSON format" should {

    "serialize to JSON correctly" in {
      val audit = MobilePinAudit(pin = "567856", deviceId = "device-xyz")
      val json = Json.toJson(audit)

      val expectedJson = Json.parse("""
            {
              "pin": "567856",
              "deviceId": "device-xyz"
            }
          """)

      json shouldBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse("""
            {
              "pin": "999956",
              "deviceId": "device-123"
            }
          """)

      val result = json.validate[MobilePinAudit]

      result.isSuccess shouldBe true
      result.get       shouldBe MobilePinAudit("999956", "device-123")
    }

    "fail to deserialize if required fields are missing" in {
      val jsonMissingPin = Json.parse("""
            {
              "deviceId": "device-abc"
            }
          """)

      val result = jsonMissingPin.validate[MobilePinAudit]
      result.isError shouldBe true
    }
  }
}
