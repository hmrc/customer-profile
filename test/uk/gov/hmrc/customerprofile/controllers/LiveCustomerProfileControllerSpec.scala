/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, EmailNotExist, EmailUpdateOk, EntityResolverConnector, HttpClientV2Helper, NoPreferenceExists, PreferencesConnector, PreferencesCreated, PreferencesDoesNotExist, PreferencesExists, PreferencesFailure, PreferencesStatus, ShutteringConnector}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.*
import uk.gov.hmrc.customerprofile.services.{AuditService, CustomerProfileService}
import uk.gov.hmrc.customerprofile.utils.AuthAndShutterMock
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class LiveCustomerProfileControllerSpec extends AuthAndShutterMock with BeforeAndAfterEach {
  val customerProfileService: CustomerProfileService = mock[CustomerProfileService]

  implicit val shutteringConnectorMock: ShutteringConnector =
    new ShutteringConnector(http = mockHttpClient, serviceUrl = s"http://baseUrl")

  val liveCustomerProfileController: LiveCustomerProfileController =
    new LiveCustomerProfileController(
      mockAuthConnector,
      200,
      customerProfileService,
      citizenDetailsEnabled = true,
      components,
      shutteringConnectorMock,
      optInVersionsEnabled = false
    )

  def mockGetPersonalDetails(response: Future[PersonDetails]) =
    when(customerProfileService.getPersonalDetails(any())(any(), any()))
      .thenReturn(response)

  def mockGetPreferences(response: Future[Option[Preference]]) =
    when(customerProfileService.getPreferences()(any(), any()))
      .thenReturn(response)

  def mockReOptInEnabledCheck(response: Preference) =
    when(customerProfileService.reOptInEnabledCheck(any()))
      .thenReturn(response)

  override def beforeEach(): Unit = {
    reset(customerProfileService)
    reset(mockAuthConnector)
  }

  "getPersonalDetails" should {

    "return personal details with journey id" in {

      mockAuthAccessAndNotShuttered()
      mockGetPersonalDetails(Future.successful(person))

      val result =
        liveCustomerProfileController.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result) mustBe toJson(person)
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result =
        liveCustomerProfileController.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result =
        liveCustomerProfileController.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveCustomerProfileController.getPersonalDetails(nino, journeyId)(
        requestWithoutAcceptHeader
      )
      status(result) mustBe 406
    }

    "return 500 for an unexpected error" in {

      mockAuthAccessAndNotShuttered()
      mockGetPersonalDetails(Future.failed(new RuntimeException()))

      val result =
        liveCustomerProfileController.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) mustBe 500
    }

    "return 521 when shuttered" in {
      mockAuthAccessAndShuttered()

      val result =
        liveCustomerProfileController.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message")
        .as[String] mustBe "Customer-Profile is currently not available"
    }

  }
  "getPreferences" should {

    "return preferences with journeyId" in {
      val preference: Preference = Preference(digital = true)

      mockAuthAccessAndNotShuttered()
      mockGetPreferences(Future.successful(Some(preference)))
      mockReOptInEnabledCheck(preference)

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result) mustBe toJson(preference)
    }

    "handle no preferences found" in {

      mockAuthAccessAndNotShuttered()
      mockGetPreferences(Future.successful(None))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 404
    }

    "propagate 401" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return Unauthorized if failed to grant access" in {
      mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 403, 403))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return Forbidden if failed to match URL NINO against Auth NINO" in {

      mockAuthorisationGrantAccessFail(new FailToMatchTaxIdOnAuth("ERROR"))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 403
    }

    "return Unauthorized if Account with low CL" in {
      mockAuthorisationGrantAccessFail(new AccountWithLowCL("ERROR"))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return 401 if the user has no nino" in {
      mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result =
        liveCustomerProfileController.getPreferences(journeyId)(requestWithoutAcceptHeader)
      status(result) mustBe 406
    }

    "return 500 for an unexpected error" in {

      mockAuthAccessAndNotShuttered()
      mockGetPreferences(Future.failed(new RuntimeException()))

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) mustBe 500
    }

    "return 521 when shuttered" in {

      mockAuthAccessAndShuttered()

      val result = liveCustomerProfileController.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message")
        .as[String] mustBe "Customer-Profile is currently not available"
    }

  }

  "paperlessSettingsOptIn/OUt and Email" should {

    val mockCitizenDetailsConnector: CitizenDetailsConnector =
      mock[CitizenDetailsConnector]
    val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
    val mockEntityResolver: EntityResolverConnector = mock[EntityResolverConnector]
    val mockAuthRetrievals: AuthRetrievals = mock[AuthRetrievals]
    val mockAuditService: AuditService = mock[AuditService]

    val service =
      new CustomerProfileService(
        mockCitizenDetailsConnector,
        mockPreferencesConnector,
        mockEntityResolver,
        mockAuthRetrievals,
        "customer-profile",
        true,
        mockAuditService
      )
    val liveCustomerProfileControllerNew: LiveCustomerProfileController =
      new LiveCustomerProfileController(
        mockAuthConnector,
        200,
        service,
        citizenDetailsEnabled = true,
        components,
        shutteringConnectorMock,
        optInVersionsEnabled = false
      )

    def mockPaperlessSettingOptIn(
      updatedPref: Option[Preference],
      result: Future[PreferencesStatus]
    ) = {
      when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
        .thenReturn(result)
      when(mockEntityResolver.getPreferences()(any(), any())).thenReturn(Future.successful(updatedPref))
      when(mockEntityResolver.paperlessSettings(any())(any(), any())).thenReturn(result)
    }
    "paperlessSettingsOptIn" should {

      "opt in for a user with no preferences with journey id" in {
        val updatedPref: Option[Preference] = Some(
          existingDigitalPreference.copy(status = Some(PaperlessStatus(StatusName.ReOptIn, Category.ReOptInRequired)))
        )
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(updatedPref, Future.successful(PreferencesCreated))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 201
      }

      "opt in with versions enabled sends version info" in {

        val controller = new LiveCustomerProfileController(
          mockAuthConnector,
          200,
          service,
          citizenDetailsEnabled = true,
          components,
          shutteringConnectorMock,
          optInVersionsEnabled = true
        )

        val updatedPref = Some(
          existingDigitalPreference.copy(status = Some(PaperlessStatus(StatusName.ReOptIn, Category.ReOptInRequired)))
        )
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(updatedPref, Future.successful(PreferencesCreated))

        val result =
          controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)
        status(result) mustBe 201
      }

      "opt in for a user with existing preferences" in {

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(Some(existingDigitalPreference), Future.successful(PreferencesExists))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 204
      }

      "return 404 where preferences do not exist" in {

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(Some(existingDigitalPreference), Future.successful(NoPreferenceExists))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 404
      }

      "return 409 for request without email" in {

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(Some(existingDigitalPreference), Future.successful(EmailNotExist))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 409
      }

      "propagate errors from the service" in {

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(Some(existingDigitalPreference), Future.successful(PreferencesFailure))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 500
      }

      "propagate 401 for auth failure" in {
        mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )
        status(result) mustBe 401
      }

      "return 401 if the user has no nino" in {
        mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )
        status(result) mustBe 401
      }

      "return status code 406 when no accept header is provided" in {
        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          paperlessSettingsRequestWithoutAcceptHeader
        )
        status(result) mustBe 406
      }

      "return 400 for an invalid form" in {
        mockAuthAccessAndNotShuttered()
        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(invalidPostRequest)
        status(result) mustBe 400
      }

      "return 500 for an unexpected error" in {

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingOptIn(Some(existingDigitalPreference), Future.failed(new RuntimeException()))

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )
        status(result) mustBe 500
      }

      "return 521 when shuttered" in {

        mockAuthAccessAndShuttered()

        val result = liveCustomerProfileControllerNew.paperlessSettingsOptIn(journeyId)(
          validPaperlessSettingsRequest
        )

        status(result) mustBe 521
        val jsonBody = contentAsJson(result)
        (jsonBody \ "shuttered").as[Boolean] mustBe true
        (jsonBody \ "title").as[String] mustBe "Shuttered"
        (jsonBody \ "message")
          .as[String] mustBe "Customer-Profile is currently not available"
      }
    }

    "paperlessSettingsOptOut" should {
      val optOutPaperlessSettings = PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))

      def optOutPaperlessSettingsWithVersion(pageType: PageType) =
        PaperlessOptOut(Some(TermsAccepted(Some(false), Some(OptInPage(Version(1, 1), 44, pageType)))), Some(English))

      def validPaperlessOptOutRequest(pageType: PageType): FakeRequest[JsValue] =
        FakeRequest()
          .withBody(toJson(optOutPaperlessSettingsWithVersion(pageType)))
          .withHeaders(HeaderNames.ACCEPT -> acceptheader)

      val optOutPaperlessSettingsRequestWithoutAcceptHeader: FakeRequest[JsValue] =
        FakeRequest().withBody(toJson(optOutPaperlessSettings))

      def mockPaperlessSettingsOptOut(result: Future[PreferencesStatus]) = {
        when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
          .thenReturn(result)
        when(mockEntityResolver.getPreferences()(any(), any()))
          .thenReturn(Future.successful(Some(existingDigitalPreference)))
        when(mockEntityResolver.paperlessSettings(any())(any(), any())).thenReturn(result)
      }

      "opt out for existing preferences with journey id" in {
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future successful PreferencesExists)
        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidReOptOutPage)
          )

        status(result) mustBe 204
      }

      "opt out without existing preferences and journey id" in {
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future successful PreferencesCreated)
        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )

        status(result) mustBe 201
      }

      "opt out with versions enabled sends version info" in {
        val controller: LiveCustomerProfileController =
          new LiveCustomerProfileController(
            mockAuthConnector,
            200,
            service,
            citizenDetailsEnabled = true,
            components,
            shutteringConnectorMock,
            optInVersionsEnabled = true
          )

        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future successful PreferencesCreated)

        val result =
          controller.paperlessSettingsOptOut(journeyId)(validPaperlessOptOutRequest(PageType.IosReOptOutPage))

        status(result) mustBe 201
      }

      "return 404 where preference does not exist" in {
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future successful PreferencesDoesNotExist)

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )

        status(result) mustBe 404
      }

      "return 500 on service error" in {
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future successful PreferencesFailure)

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )

        status(result) mustBe 500
      }

      "propagate 401 for auth failure" in {

        mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )
        status(result) mustBe 401
      }

      "return 401 if the user has no nino" in {
        mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )
        status(result) mustBe 401
      }

      "return status code 406 when no accept header is provided" in {
        val result = liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
          optOutPaperlessSettingsRequestWithoutAcceptHeader
        )
        status(result) mustBe 406
      }

      "return 500 for an unexpected error" in {
        mockAuthAccessAndNotShuttered()
        mockPaperlessSettingsOptOut(Future failed new RuntimeException())

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )
        status(result) mustBe 500
      }

      "return 521 when shuttered" in {
        mockAuthAccessAndShuttered()

        val result =
          liveCustomerProfileControllerNew.paperlessSettingsOptOut(journeyId)(
            validPaperlessOptOutRequest(PageType.AndroidOptOutPage)
          )

        status(result) mustBe 521
        val jsonBody = contentAsJson(result)
        (jsonBody \ "shuttered").as[Boolean] mustBe true
        (jsonBody \ "title").as[String] mustBe "Shuttered"
        (jsonBody \ "message")
          .as[String] mustBe "Customer-Profile is currently not available"
      }
    }

    "preferencesPendingEmail" should {

      val newEmail = EmailAddress("new@new.com")
      val changeEmail = ChangeEmail(newEmail)

      val validPendingEmailRequest: FakeRequest[JsValue] =
        FakeRequest()
          .withBody(toJson(changeEmail))
          .withHeaders(HeaderNames.ACCEPT -> acceptheader)
      val changeEmailRequestWithoutAcceptHeader: FakeRequest[JsValue] =
        FakeRequest().withBody(toJson(changeEmail))

      def mockGetNiNo(result: Future[Option[Nino]]) = {
        when(mockAuditService.withAudit[Option[Nino]](any(), any())(any())(any(), any()))
          .thenReturn(result)
        when(mockAuthRetrievals.retrieveNino()(any(), any())).thenReturn(result)
      }

      def mockPendingEmail(result: Future[PreferencesStatus]) = {
        mockGetNiNo(Future.successful(Some(nino)))
        when(mockAuditService.withAudit[PreferencesStatus](any(), any())(any())(any(), any()))
          .thenReturn(result)
        when(mockEntityResolver.getEntityIdByNino(any())(any(), any())).thenReturn(Future.successful(entity))
        when(mockPreferencesConnector.updatePendingEmail(any(), any())(any(), any())).thenReturn(result)

      }

      "successful pending email change" in {
        mockAuthAccessAndNotShuttered()
        mockPendingEmail(Future.successful(EmailUpdateOk))

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

        status(result) mustBe 204
      }

      "return 404 where preferences do not exist" in {
        mockAuthAccessAndNotShuttered()
        mockPendingEmail(Future successful NoPreferenceExists)

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
        status(result) mustBe 404
      }

      "return 409 for request without email" in {
        mockAuthAccessAndNotShuttered()
        mockPendingEmail(Future successful EmailNotExist)

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

        status(result) mustBe 409
      }

      "propagate errors from the service" in {
        mockAuthAccessAndNotShuttered()
        mockPendingEmail(Future successful PreferencesFailure)

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

        status(result) mustBe 500
      }

      "propagate 401 for auth failure" in {

        mockAuthorisationGrantAccessFail(UpstreamErrorResponse("ERROR", 401, 401))
        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
        status(result) mustBe 401
      }

      "return 401 if the user has no nino" in {
        mockAuthorisationGrantAccessFail(new NinoNotFoundOnAccount("no nino"))

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
        status(result) mustBe 401
      }

      "return status code 406 when no accept header is provided" in {
        val result = liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(
          changeEmailRequestWithoutAcceptHeader
        )
        status(result) mustBe 406
      }

      "return 400 for an invalid form" in {
        mockAuthAccessAndNotShuttered()
        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(invalidPostRequest)
        status(result) mustBe 400
      }

      "return 500 for an unexpected error" in {
        mockAuthAccessAndNotShuttered()
        mockPendingEmail(Future failed new RuntimeException())

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
        status(result) mustBe 500
      }

      "return 521 when shuttered" in {
        mockAuthAccessAndShuttered()

        val result =
          liveCustomerProfileControllerNew.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

        status(result) mustBe 521
        val jsonBody = contentAsJson(result)
        (jsonBody \ "shuttered").as[Boolean] mustBe true
        (jsonBody \ "title").as[String] mustBe "Shuttered"
        (jsonBody \ "message")
          .as[String] mustBe "Customer-Profile is currently not available"
      }
    }

  }

}
