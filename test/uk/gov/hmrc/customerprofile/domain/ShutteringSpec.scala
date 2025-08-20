package uk.gov.hmrc.customerprofile.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ShutteringSpec extends AnyFlatSpec with Matchers {
  "Shuttering" should "serialize and deserialize correctly with all the feilds" in {
    val shuttering = Shuttering(
      shuttered = true,
      title = Some("Service Unavailable"),
      message = Some("Try again later"),
      titleCy = Some("Gwasanaeth ddim ar gael"),
      messageCy = Some("Rhowch gynnig arall yn hwyrach")
    )
    val json = Json.toJson(shuttering)
    val deserialize = json.as[Shuttering]

    deserialize shouldEqual shuttering
  }
  it should "return a diabled shuttering instance using shutteringDisabled" in {
    val disabled = Shuttering.shutteringDisabled

    disabled.shuttered shouldBe false
    disabled.title shouldBe None
    disabled.message shouldBe None
    disabled.titleCy shouldBe None
    disabled.messageCy shouldBe None

  }
}
