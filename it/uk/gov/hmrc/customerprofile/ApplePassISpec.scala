package uk.gov.hmrc.customerprofile

import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import uk.gov.hmrc.customerprofile.domain.Shuttering
import uk.gov.hmrc.customerprofile.stubs.AuthStub.{authFailure, authRecordExists, ninoFound}
import uk.gov.hmrc.customerprofile.stubs.CitizenDetailsStub.{designatoryDetailsForNinoAre, designatoryDetailsWillReturnErrorResponse}
import uk.gov.hmrc.customerprofile.stubs.ShutteringStub.{stubForShutteringDisabled, stubForShutteringEnabled}
import uk.gov.hmrc.customerprofile.stubs.FindmyNinoWalletStub.{getApplePass, getApplePassId, getApplePassIdTooManyRequestsException}
import uk.gov.hmrc.customerprofile.support.BaseISpec

class ApplePassISpec extends BaseISpec {
  val base64String = "TXIgSm9lIEJsb2dncw=="
  val passId       = "c864139e-77b5-448f-b443-17c69060870d"
  val fullName     = "Mr Angus John Smith"

  "GET  /apple-pass" should {
    val url = s"/apple-pass?journeyId=$journeyId"

    "return the apple pass with the journey id" in {
      authRecordExists(nino)
      stubForShutteringDisabled
      ninoFound(nino)
      designatoryDetailsForNinoAre(nino, resourceAsString("AA000006C-citizen-details.json").get)
      getApplePassId(nino, fullName, passId)
      getApplePass(passId, base64String)
      val response = await(getRequestWithAcceptHeader(url))
      response.status                          shouldBe 200
      (response.json \ "applePass").as[String] shouldBe base64String
    }

    "return a 429 if too many calls are made to apple pass and it returns a 429" in {
      authRecordExists(nino)
      stubForShutteringDisabled
      ninoFound(nino)
      designatoryDetailsForNinoAre(nino, resourceAsString("AA000006C-citizen-details.json").get)
      getApplePassIdTooManyRequestsException(nino, fullName)
      val response = await(getRequestWithAcceptHeader(url))
      response.status shouldBe 429
    }

    "return 406 if no request header is supplied" in {
      await(wsUrl(url).get()).status shouldBe 406
    }

    "propagate 401" in {
      authFailure()
      stubForShutteringDisabled
      await(getRequestWithAcceptHeader(url)).status shouldBe 401
    }

    "return 400 if no journeyId is supplied" in {
      await(wsUrl("/apple-pass").get()).status shouldBe 400
    }

    "return 400 if invalid journeyUd is supplied" in {
      await(wsUrl("/apple-pass?journeyId=ThisIsAnInvalidJourneyId").get()).status shouldBe 400
    }

    "return shuttered when shuttered" in {
      stubForShutteringEnabled
      authRecordExists(nino)

      val response = await(getRequestWithAcceptHeader(url))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Preferences are currently not available")
    }

    "return 404 response status code when citizen-details returns 404 response status code." in {
      designatoryDetailsWillReturnErrorResponse(nino, 404)
      authRecordExists(nino)
      stubForShutteringDisabled

      val response = await(getRequestWithAcceptHeader(url))
      response.status shouldBe 404
      response.json   shouldBe parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")
    }
  }
}
