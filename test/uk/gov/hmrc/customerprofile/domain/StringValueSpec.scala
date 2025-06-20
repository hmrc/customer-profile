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
import uk.gov.hmrc.customerprofile.emailaddress.StringValue
import uk.gov.hmrc.customerprofile.utils.BaseSpec

class StringValueSpec extends BaseSpec {

  "StringValue" should {

    case class TestStringValue(value: String) extends StringValue

    "return correct value from .value" in {
      val sv = TestStringValue("hello")
      sv.value shouldBe "hello"
    }

    "return correct value from .toString" in {
      val sv = TestStringValue("world")
      sv.toString shouldBe "world"
    }

    "convert implicitly to String" in {
      val sv = TestStringValue("implicit")

      // import implicit conversion
      import StringValue.stringValueToString

      val result: String = sv // implicit conversion triggered
      result shouldBe "implicit"
    }
  }

}
