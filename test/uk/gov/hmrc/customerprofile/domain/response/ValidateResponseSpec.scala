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

package uk.gov.hmrc.customerprofile.domain.response

import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*
import uk.gov.hmrc.customerprofile.response.ValidateResponse
import uk.gov.hmrc.customerprofile.utils.BaseSpec

class ValidateResponseSpec extends BaseSpec {

  "ValidateResponse JSON format" should {

    "serialize ValidateResponse with key" in {
      val response = ValidateResponse(Some("some-key"), "All good")
      val json = Json.toJson(response)

      val expectedJson = Json.parse("""
           {
             "key": "some-key",
             "message": "All good"
           }
         """)

      json shouldBe expectedJson
    }

    "serialize ValidateResponse without key (None)" in {
      val response = ValidateResponse(None, "Message without key")
      val json = Json.toJson(response)

      val expectedJson = Json.parse("""
           {
             "message": "Message without key"
           }
         """)

      json shouldBe expectedJson
    }

    "deserialize JSON with key" in {
      val json = Json.parse("""
           {
             "key": "abc123",
             "message": "Success"
           }
         """)

      val result = json.validate[ValidateResponse]

      result.isSuccess shouldBe true
      result.get       shouldBe ValidateResponse(Some("abc123"), "Success")
    }

    "deserialize JSON without key" in {
      val json = Json.parse("""
           {
             "message": "Only message"
           }
         """)

      val result = json.validate[ValidateResponse]

      result.isSuccess shouldBe true
      result.get       shouldBe ValidateResponse(None, "Only message")
    }

    "fail to deserialize if message is missing" in {
      val json = Json.parse("""
           {
             "key": "xyz"
           }
         """)

      val result = json.validate[ValidateResponse]
      result.isError shouldBe true
    }
  }
}
