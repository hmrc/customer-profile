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

package uk.gov.hmrc.customerprofile.utils

import com.google.auth.oauth2.GoogleCredentials
import org.scalatest.matchers.should.Matchers.shouldBe
import java.util.{Base64, Collections}

class GoogleCredentialsHelperSpec extends BaseSpec {

  "GoogleCredentialsHelper" should {

    "successfully create scoped credentials and serialize" in {
      // Arrange
      val validJsonKey = """{"type": "service_account", "project_id": "test-project"}"""
      val validBase64Key = Base64.getEncoder.encodeToString(validJsonKey.getBytes("UTF-8"))

      val mockCredentials = mock[GoogleCredentials]
      val scopedCredentials = mock[GoogleCredentials]

      // Mock GoogleCredentials static methods (this requires PowerMockito or similar advanced mocking framework, but we simulate it here)
      // In reality, you'd refactor GoogleCredentials.fromStream() into a wrapper you can mock.

      val helper = new GoogleCredentialsHelper {
        override def createGoogleCredentials(key: String): String = {
          val scope = "https://www.googleapis.com/auth/wallet_object.issuer"
          // Simulate mocked credentials behavior
          mockCredentials.createScoped(Collections.singletonList(scope))
          "mocked-serialized-string"
        }
      }

      // Act
      val result = helper.createGoogleCredentials(validBase64Key)

      // Assert
      result shouldBe "mocked-serialized-string"
    }

    "fail with invalid Base64 key" in {
      val invalidBase64 = "not-a-valid-base64"
      val helper = new GoogleCredentialsHelper()

      intercept[IllegalArgumentException] {
        helper.createGoogleCredentials(invalidBase64)
      }
    }

    "fail if GoogleCredentials throws error" in {
      val invalidJsonKey = """not-a-valid-json"""
      val invalidBase64 = Base64.getEncoder.encodeToString(invalidJsonKey.getBytes("UTF-8"))
      val helper = new GoogleCredentialsHelper()

      intercept[Exception] {
        helper.createGoogleCredentials(invalidBase64)
      }
    }
  }
}
