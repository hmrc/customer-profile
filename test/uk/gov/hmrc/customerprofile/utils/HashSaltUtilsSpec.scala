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

import org.mindrot.jbcrypt.BCrypt
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HashSaltUtilsSpec extends AnyWordSpec with Matchers {

  "HashSaltUtilsSpec" should {

    "return true if the string matched the hashed string" in {
      val hashed  = HashSaltUtils.createHashAndSalt("30061986")
      val isValid = BCrypt.checkpw("30061986", hashed)
      isValid shouldBe (true)
    }

    "return true if the string is present in hashed list" in {

      val string1 = "30061986"
      val hash1   = HashSaltUtils.createHashAndSalt(string1)

      val string2 = "24072012"
      val hash2   = HashSaltUtils.createHashAndSalt(string2)

      val hashedList = List(hash1, hash2)

      hashedList.exists(storedHash => BCrypt.checkpw("30061986", storedHash)) shouldBe (true)

    }

    "return false if the string is not present in hashed list" in {

      val string1 = "30061986"
      val hash1   = HashSaltUtils.createHashAndSalt(string1)

      val string2 = "24072012"
      val hash2   = HashSaltUtils.createHashAndSalt(string2)

      val hashedList = List(hash1, hash2)

      hashedList.exists(storedHash => BCrypt.checkpw("04061985", storedHash)) shouldBe (false)

    }
  }

}
