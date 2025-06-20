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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class DOBUtilsSpec extends AnyWordSpec with Matchers {
  val dobBirth1 = Some(LocalDate.of(1985, 12, 11))
  val dobBirth2 = Some(LocalDate.of(1986, 11, 3))
  val dobBirth3 = Some(LocalDate.of(1986, 4, 10))
  val dobBirth4 = Some(LocalDate.of(1985, 4, 2))

  "DOBUtilsSpec" should {

    "return true" when {

      "date of Birth is 11-12-1985" when {

        "entered pin is 111285" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "111285") shouldBe true
        }

        "entered pin is 118512" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "118512") shouldBe true
        }

        "entered pin is 121185" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "121185") shouldBe true
        }

        "entered pin is 128511" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "128511") shouldBe true
        }

        "entered pin is 851211" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "851211") shouldBe true
        }

        "entered pin is 851112" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "851112") shouldBe true
        }

        "entered pin is 198512" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "198512") shouldBe true
        }

        "entered pin is 198511" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "198511") shouldBe true
        }

        "entered pin is 21985" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "121985") shouldBe true
        }

        "entered pin is 111985" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "111985") shouldBe true
        }
      }

      "date of Birth is 03-11-1986" when {

        "entered pin is 031186" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "031186") shouldBe true
        }
        "entered pin is 038611" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "038611") shouldBe true
        }

        "entered pin is 110386" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "110386") shouldBe true
        }
        "entered pin is 118603" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "118603") shouldBe true
        }

        "entered pin is 861103" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "861103") shouldBe true
        }

        "entered pin is 860311" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "860311") shouldBe true
        }

        "entered pin is 198603" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "198603") shouldBe true
        }

        "entered pin is 198611" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "198611") shouldBe true
        }

        "entered pin is 111986" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "111986") shouldBe true
        }

        "entered pin is 031986" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "031986") shouldBe true
        }

      }

      "date of Birth is 10-04-1986" when {

        "entered pin is 100486" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "100486") shouldBe true
        }

        "entered pin is 108604" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "108604") shouldBe true
        }

        "entered pin is 041086" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "041086") shouldBe true
        }

        "entered pin is 048610" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "048610") shouldBe true
        }

        "entered pin is 861004" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "861004") shouldBe true
        }

        "entered pin is 860410" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "860410") shouldBe true
        }

        "entered pin is 198604" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "198604") shouldBe true
        }

        "entered pin is 198610" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "198610") shouldBe true
        }

        "entered pin is 101986" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "101986") shouldBe true
        }

        "entered pin is 041986" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "041986") shouldBe true
        }

      }

      "date of Birth is 02-04-1985" when {

        "entered pin is 020485" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "020485") shouldBe true
        }

        "entered pin is 028504" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "028504") shouldBe true
        }

        "entered pin is 040285" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "040285") shouldBe true
        }

        "entered pin is 048502" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "048502") shouldBe true
        }

        "entered pin is 850402" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "850402") shouldBe true
        }

        "entered pin is 860204" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "850204") shouldBe true
        }

        "entered pin is 198504" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "198504") shouldBe true
        }

        "entered pin is 198502" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "198502") shouldBe true
        }

        "entered pin is 021985" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "021985") shouldBe true
        }

        "entered pin is 041985" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "041985") shouldBe true
        }

        "entered pin is 241985" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "241985") shouldBe true
        }

        "entered pin is 421985" in {
          DOBUtils.matchesDOBPatterns(dobBirth4, "421985") shouldBe true
        }

      }
    }

    "return false" when {

      "date of Birth is 11-12-1985" when {

        "entered pin is 111085" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "111085") shouldBe false
        }

        "entered pin is 101285" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "101285") shouldBe false
        }

        "entered pin is 111286" in {
          DOBUtils.matchesDOBPatterns(dobBirth1, "111286") shouldBe false
        }
      }

      "date of Birth is 03-11-1986" when {

        "entered pin is 031185" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "031185") shouldBe false
        }

        "entered pin is 861102" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "861102") shouldBe false
        }

        "entered pin is 860103" in {
          DOBUtils.matchesDOBPatterns(dobBirth2, "860103") shouldBe false
        }
      }

      "date of Birth is 10-04-1986" when {

        "entered pin is 041085" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "041085") shouldBe false
        }

        "entered pin is 100586" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "100586") shouldBe false
        }

        "entered pin is 861104" in {
          DOBUtils.matchesDOBPatterns(dobBirth3, "861104") shouldBe false
        }
      }

      "date of birth is None" in {

        DOBUtils.matchesDOBPatterns(None, "111085") shouldBe false

      }
    }

  }

}
