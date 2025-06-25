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

import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.customerprofile.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.customerprofile.utils.BaseSpec

class ObfuscatedEmailAddressSpec extends BaseSpec {

  "ObfuscatedEmailAddress" should {

    "obfuscate short local part (<= 2 characters)" in {
      val email = "ab@domain.com"
      val obfuscated = ObfuscatedEmailAddress(email)
      obfuscated.value shouldBe "**@domain.com"
    }

    "obfuscate long local part (> 2 characters)" in {
      val email = "john.doe@domain.com"
      val obfuscated = ObfuscatedEmailAddress(email)
      obfuscated.value shouldBe "j******e@domain.com"
    }

    "obfuscate local part of exactly 3 characters" in {
      val email = "abc@domain.com"
      val obfuscated = ObfuscatedEmailAddress(email)
      obfuscated.value shouldBe "a*c@domain.com"
    }

    "handle email with subdomains" in {
      val email = "john.doe@sub.domain.com"
      val obfuscated = ObfuscatedEmailAddress(email)
      obfuscated.value shouldBe "j******e@sub.domain.com"
    }

    "handle one character mailbox" in {
      val email = "a@domain.com"
      val obfuscated = ObfuscatedEmailAddress(email)
      obfuscated.value shouldBe "*@domain.com"
    }
  }
}
