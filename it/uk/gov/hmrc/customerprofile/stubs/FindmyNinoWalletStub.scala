package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, get, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.urlEqualToDesignatoryDetails
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.Json.obj
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass

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
            |"fullName": "$name"
            |}""".stripMargin))
        .willReturn(aResponse().withStatus(200)
          .withBody(uuid))
    )

  def getApplePass(uuid: String, applePass: String): StubMapping =
    stubFor(
      get(urlEqualToGetPassCard(uuid))
        .willReturn(aResponse().withStatus(200)
          .withBody(applePass))
    )

private def urlEqualToCreateApplePass() : UrlPattern = urlEqualTo(s"/find-my-nino-add-to-wallet/create-apple-pass")
private def urlEqualToGetPassCard(passId: String) : UrlPattern =
  urlEqualTo(s"/find-my-nino-add-to-wallet/get-pass-card?passId=$passId")



}
