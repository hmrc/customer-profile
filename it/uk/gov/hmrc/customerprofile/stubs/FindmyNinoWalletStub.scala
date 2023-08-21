package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, get, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.urlEqualToDesignatoryDetails
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.Json.{obj, parse}
import uk.gov.hmrc.customerprofile.domain.RetrieveApplePass

object FindmyNinoWalletStub {

  def getApplePassId(
                        nino: Nino,
                        name: String,
                        passId: String
                        ): StubMapping =
    stubFor(
      post(urlEqualToCreateApplePass())
        .withRequestBody(equalToJson(
          s"""{
            |"nino": "${nino.formatted}",
            |"fullName": "$name"
            |}""".stripMargin))
        .willReturn(aResponse().withStatus(200)
          .withBody(passId))
    )

  def getApplePassIdTooManyRequestsException(
                      nino: Nino,
                      name: String
                    ): StubMapping =
    stubFor(
      post(urlEqualToCreateApplePass())
        .withRequestBody(equalToJson(
          s"""{
             |"nino": "${nino.formatted}",
             |"fullName": "$name"
             |}""".stripMargin))
        .willReturn(aResponse().withStatus(429)
          .withBody(
          s"""
             |{
             |  "code": "TOO_MANY_REQUESTS"
             |  "message": "Too many requests made to customer profile please try again later"
             |}
        """.stripMargin)))

  def getApplePass(passId: String, applePass: String): StubMapping =
    stubFor(
      get(urlEqualToGetPassCard(passId))
        .willReturn(aResponse().withStatus(200)
          .withBody(applePass))
    )

  def createGooglePassWithCredentials(
                      nino: Nino,
                      name: String,
                      credentials: String,
                      passId: String
                    ): StubMapping =
    stubFor(
      post(urlEqualToCreateGooglePass())
        .withRequestBody(equalToJson(
          s"""{
             |"nino": "${nino.formatted}",
             |"fullName": "$name",
             |"credentials": "$credentials"
             |}""".stripMargin))
        .willReturn(aResponse().withStatus(200)
          .withBody(passId))
    )

  def getGooglePass(passId: String, googlePass: String): StubMapping =
    stubFor(
      get(urlEqualToGetGooglePass(passId))
        .willReturn(aResponse().withStatus(200)
          .withBody(googlePass))
    )

private def urlEqualToCreateApplePass() : UrlPattern = urlEqualTo(s"/find-my-nino-add-to-wallet/create-apple-pass")
  private def urlEqualToGetPassCard(passId: String) : UrlPattern = {
  urlEqualTo(s"/find-my-nino-add-to-wallet/get-pass-card?passId=$passId")
}

  private def urlEqualToCreateGooglePass(): UrlPattern = urlEqualTo(s"/find-my-nino-add-to-wallet/create-google-pass-with-credentials")

  private def urlEqualToGetGooglePass(passId: String): UrlPattern = urlEqualTo(s"/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId")


}
