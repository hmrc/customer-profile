/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.controllers

import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.connector.{EmailNotExist, EmailUpdateOk, NoPreferenceExists, PreferencesCreated, PreferencesDoesNotExist, PreferencesExists, PreferencesFailure, PreferencesStatus}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.services.CustomerProfileService
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class LiveCustomerProfileControllerSpec extends BaseSpec {
  val service:                    CustomerProfileService = mock[CustomerProfileService]
  implicit val mockAuthConnector: AuthConnector          = mock[AuthConnector]

  val controller: LiveCustomerProfileController =
    new LiveCustomerProfileController(
      mockAuthConnector,
      200,
      service,
      citizenDetailsEnabled = true,
      stubControllerComponents(),
      shutteringConnectorMock,
      optInVersionsEnabled = false
    )

  "getPersonalDetails" should {
    def mockGetAccounts(result: Future[PersonDetails]) =
      (service
        .getPersonalDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *)
        .returns(result)

    "return personal details with journey id" in {
      val person = PersonDetails(
        Person(
          Some("Firstname"),
          Some("Middle"),
          Some("Lastname"),
          Some("Intial"),
          Some("Title"),
          Some("Honours"),
          Some("sex"),
          None,
          None,
          None,
          Some("Firstname Lastname"),
          Some("/personal-account/national-insurance-summary/save-letter-as-pdf")
        ),
        Some(Address(changeAddressLink = Some("/personal-account/your-profile"))),
        None
      )

      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetAccounts(Future successful person)
      mockShutteringResponse(notShuttered)

      val result =
        controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(person)
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getPersonalDetails(nino, journeyId)(
        requestWithoutAcceptHeader
      )
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetAccounts(Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }

    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result =
        controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }
  }

  "getPreferences" should {
    def mockGetPreferences(result: Future[Option[Preference]]) =
      (service
        .getPreferences()(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *)
        .returns(result)

    def mockReOptInCheck(result: Preference) =
      (service
        .reOptInEnabledCheck(_: Preference))
        .expects(*)
        .returns(result)

    "return preferences with journeyId" in {
      val preference: Preference =
        Preference(
          digital = true
        )

      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPreferences(Future successful Some(preference))
      mockReOptInCheck(preference)
      mockShutteringResponse(notShuttered)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(preference)
    }

    "handle no preferences found" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPreferences(Future successful None)
      mockShutteringResponse(notShuttered)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 404
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return Unauthorized if failed to grant access" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {
      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return Unauthorized if Account with low CL" in {
      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result =
        controller.getPreferences(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPreferences(Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }

    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }
  }

  "paperlessSettingsOptIn" should {
    def mockPaperlessSettings(
      settings: Paperless,
      result:   Future[PreferencesStatus]
    ) =
      (service
        .paperlessSettings(_: Paperless, _: JourneyId)(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(settings, *, *, *)
        .returns(result)

    val newEmail          = EmailAddress("new@new.com")
    val paperlessSettings = Paperless(TermsAccepted(Some(true)), newEmail, Some(English))
    val paperlessSettingsWithVersion =
      Paperless(TermsAccepted(accepted = Some(true), Some(OptInPage(Version(1, 1), 44, PageType.AndroidOptInPage))),
                newEmail,
                Some(English))

    val validPaperlessSettingsRequest: FakeRequest[JsValue] =
      FakeRequest()
        .withBody(toJson(paperlessSettingsWithVersion))
        .withHeaders(HeaderNames.ACCEPT → acceptheader)

    val paperlessSettingsRequestWithoutAcceptHeader: FakeRequest[JsValue] =
      FakeRequest().withBody(toJson(paperlessSettings))

    "opt in for a user with no preferences with journey id" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(
        paperlessSettings,
        Future successful PreferencesCreated
      )
      mockShutteringResponse(notShuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 201
    }

    "opt in with versions enabled sends version info" in {
      val controller: LiveCustomerProfileController =
        new LiveCustomerProfileController(
          mockAuthConnector,
          200,
          service,
          citizenDetailsEnabled = true,
          stubControllerComponents(),
          shutteringConnectorMock,
          optInVersionsEnabled = true
        )

      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(paperlessSettingsWithVersion, Future successful PreferencesCreated)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 201
    }

    "opt in for a user with existing preferences" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(
        paperlessSettings,
        Future successful PreferencesExists
      )
      mockShutteringResponse(notShuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 204
    }

    "return 404 where preferences do not exist" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(
        paperlessSettings,
        Future successful NoPreferenceExists
      )
      mockShutteringResponse(notShuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 404
    }

    "return 409 for request without email" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(paperlessSettings, Future successful EmailNotExist)
      mockShutteringResponse(notShuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 409
    }

    "propagate errors from the service" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettings(
        paperlessSettings,
        Future successful PreferencesFailure
      )
      mockShutteringResponse(notShuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )
      status(result) shouldBe 401
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.paperlessSettingsOptIn(journeyId)(
        paperlessSettingsRequestWithoutAcceptHeader
      )
      status(result) shouldBe 406
    }

    "return 400 for an invalid form" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      val result =
        controller.paperlessSettingsOptIn(journeyId)(invalidPostRequest)
      status(result) shouldBe 400
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      mockPaperlessSettings(
        paperlessSettings,
        Future failed new RuntimeException()
      )

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )
      status(result) shouldBe 500
    }

    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result = controller.paperlessSettingsOptIn(journeyId)(
        validPaperlessSettingsRequest
      )

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }
  }

  "paperlessSettingsOptOut" should {

    val optOutPaperlessSettings = PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))

    def optOutPaperlessSettingsWithVersion(pageType: PageType) =
      PaperlessOptOut(Some(TermsAccepted(Some(false), Some(OptInPage(Version(1, 1), 44, pageType)))), Some(English))

    def validPaperlessOptOutRequest(pageType: PageType): FakeRequest[JsValue] =
      FakeRequest()
        .withBody(toJson(optOutPaperlessSettingsWithVersion(pageType)))
        .withHeaders(HeaderNames.ACCEPT → acceptheader)

    val optOutPaperlessSettingsRequestWithoutAcceptHeader: FakeRequest[JsValue] =
      FakeRequest().withBody(toJson(optOutPaperlessSettings))

    def mockPaperlessSettingsOptOut(
      optOutPaperless: PaperlessOptOut,
      result:          Future[PreferencesStatus]
    ) =
      (service
        .paperlessSettingsOptOut(_: PaperlessOptOut)(_: HeaderCarrier, _: ExecutionContext))
        .expects(optOutPaperless, *, *)
        .returns(result)

    "opt out for existing preferences with journey id" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettings, Future successful PreferencesExists)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidReOptOutPage))

      status(result) shouldBe 204
    }

    "opt out without existing preferences and journey id" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettings, Future successful PreferencesCreated)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))

      status(result) shouldBe 201
    }

    "opt out with versions enabled sends version info" in {
      val controller: LiveCustomerProfileController =
        new LiveCustomerProfileController(
          mockAuthConnector,
          200,
          service,
          citizenDetailsEnabled = true,
          stubControllerComponents(),
          shutteringConnectorMock,
          optInVersionsEnabled = true
        )

      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettingsWithVersion(PageType.IosReOptOutPage),
                                  Future successful PreferencesCreated)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.IosReOptOutPage))

      status(result) shouldBe 201
    }

    "return 404 where preference does not exist" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettings, Future successful PreferencesDoesNotExist)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))

      status(result) shouldBe 404
    }

    "return 500 on service error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettings, Future successful PreferencesFailure)
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))
      status(result) shouldBe 401
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.paperlessSettingsOptOut(journeyId)(
        optOutPaperlessSettingsRequestWithoutAcceptHeader
      )
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPaperlessSettingsOptOut(optOutPaperlessSettings, Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))
      status(result) shouldBe 500
    }

    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result =
        controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.AndroidOptOutPage))

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }
  }

  "preferencesPendingEmail" should {

    def mockPendingEmail(
      changeEmail: ChangeEmail,
      result:      Future[PreferencesStatus]
    ) =
      (service
        .setPreferencesPendingEmail(_: ChangeEmail)(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(changeEmail, *, *)
        .returns(result)

    val newEmail    = EmailAddress("new@new.com")
    val changeEmail = ChangeEmail(newEmail)

    val validPendingEmailRequest: FakeRequest[JsValue] =
      FakeRequest()
        .withBody(toJson(changeEmail))
        .withHeaders(HeaderNames.ACCEPT → acceptheader)
    val changeEmailRequestWithoutAcceptHeader: FakeRequest[JsValue] =
      FakeRequest().withBody(toJson(changeEmail))

    "successful pending email change" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPendingEmail(changeEmail, Future successful EmailUpdateOk)
      mockShutteringResponse(notShuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 204
    }

    "return 404 where preferences do not exist" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPendingEmail(changeEmail, Future successful NoPreferenceExists)
      mockShutteringResponse(notShuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 404
    }

    "return 409 for request without email" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPendingEmail(changeEmail, Future successful EmailNotExist)
      mockShutteringResponse(notShuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 409
    }

    "propagate errors from the service" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPendingEmail(changeEmail, Future successful PreferencesFailure)
      mockShutteringResponse(notShuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 401
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.preferencesPendingEmail(journeyId)(
        changeEmailRequestWithoutAcceptHeader
      )
      status(result) shouldBe 406
    }

    "return 400 for an invalid form" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      val result =
        controller.preferencesPendingEmail(journeyId)(invalidPostRequest)
      status(result) shouldBe 400
    }

    "return 500 for an unexpected error" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockPendingEmail(changeEmail, Future failed new RuntimeException())
      mockShutteringResponse(notShuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 500
    }

    "return 521 when shuttered" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(shuttered)

      val result =
        controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message")
        .as[String] shouldBe "Customer-Profile is currently not available"
    }
  }

}
