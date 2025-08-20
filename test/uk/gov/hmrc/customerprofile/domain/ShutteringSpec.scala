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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ShutteringSpec extends AnyFlatSpec with Matchers {
  "Shuttering" should "serialize and deserialize correctly with all the feilds" in {
    val shuttering = Shuttering(
      shuttered = true,
      title     = Some("Service Unavailable"),
      message   = Some("Try again later"),
      titleCy   = Some("Gwasanaeth ddim ar gael"),
      messageCy = Some("Rhowch gynnig arall yn hwyrach")
    )
    val json = Json.toJson(shuttering)
    val deserialize = json.as[Shuttering]

    deserialize shouldEqual shuttering
  }
  it should "return a diabled shuttering instance using shutteringDisabled" in {
    val disabled = Shuttering.shutteringDisabled

    disabled.shuttered shouldBe false
    disabled.title     shouldBe None
    disabled.message   shouldBe None
    disabled.titleCy   shouldBe None
    disabled.messageCy shouldBe None

  }
}
