package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, get, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.urlEqualToDesignatoryDetails
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.Json.obj

object FindmyNinoWalletStub {

  def getApplePassUUID(
                        nino: Nino,
                        name: String,
                        uuid: String
                        ): StubMapping =
    stubFor(
      post(urlEqualToCreateApplePass())
        .withRequestBody(equalToJson(
          s"""{
            |"nino": "$nino",
            |"name": "$name"
            |}""".stripMargin))
        .willReturn(aResponse().withStatus(200)
          .withBody(obj("uuid" -> uuid).toString))
    )

  def getApplePass(uuid: String, applePass: String): StubMapping =
    stubFor(
      get(urlEqualToGetPassCard(uuid))
        .willReturn(aResponse().withStatus(200)
          .withBody(obj("applePass" -> applePass).toString))

    )

private def urlEqualToCreateApplePass() : UrlPattern = urlEqualTo(s"/find-my-nino-add-to-wallet/create-apple-pass")
private def urlEqualToGetPassCard(passId: String) : UrlPattern =
  urlEqualTo(s"/find-my-nino-add-to-wallet/get-pass-card?=$passId")



}
