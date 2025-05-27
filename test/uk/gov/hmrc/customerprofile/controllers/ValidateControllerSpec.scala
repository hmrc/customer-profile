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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.customerprofile.controllers
//
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import org.scalatest.BeforeAndAfterEach
//import play.api.test.Helpers._
//import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
//import org.scalatestplus.mockito.MockitoSugar.mock
//import play.api.mvc.AnyContentAsEmpty
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{contentAsJson, status, stubControllerComponents}
//import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
//import uk.gov.hmrc.auth.core.retrieve._
//import uk.gov.hmrc.auth.core.syntax.retrieved._
//import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
//import uk.gov.hmrc.customerprofile.auth.AuthRetrievals
//import uk.gov.hmrc.customerprofile.connector.{CitizenDetailsConnector, EntityResolverConnector, HttpClientV2Helper, PreferencesConnector}
//import uk.gov.hmrc.customerprofile.domain.PersonDetails
//import uk.gov.hmrc.customerprofile.services.{AuditService, CustomerProfileService, MongoService}
//import uk.gov.hmrc.customerprofile.utils.{AuthAndShutterMock, BaseSpec}
//import uk.gov.hmrc.domain.Nino
//import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
//
//import java.net.URL
//import java.time.LocalDate
//import scala.concurrent.{ExecutionContext, Future}
//
//class ValidateControllerSpec extends BaseSpec with AuthAndShutterMock with BeforeAndAfterEach with HttpClientV2Helper {
//
//  val deviceId = "6D92078A-8246-4BA4-AE5B-76104861E7DC"
//  val dob1     = LocalDate.of(1980, 7, 24)
//  val acceptHeader: (String, String)                    = "Accept" -> "application/vnd.hmrc.1.0+json"
//  val fakeRequest:  FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withHeaders(acceptHeader)
//
//  val serviceUrl: String = "https://customer-profile"
//  val mockCitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]
//  val mockCustomerProfileService: CustomerProfileService  = mock[CustomerProfileService]
//  implicit val mockMongoService:  MongoService            = mock[MongoService]
//  val mockPreferencesConnector:   PreferencesConnector    = mock[PreferencesConnector]
//  val mockEntityResolver:         EntityResolverConnector = mock[EntityResolverConnector]
//  val mockAuthRetrievals:         AuthRetrievals          = mock[AuthRetrievals]
//  val mockAuditService:           AuditService            = mock[AuditService]
//
//  val controller = new ValidateController(authConnector = mockAuthConnector,
//                                          citizenDetailsConnector = mockCitizenDetailsConnector,
//                                          mongoService            = mockMongoService,
//                                          customerProfileService  = mockCustomerProfileService,
//                                          confLevel               = 200,
//                                          storedPinCount          = 3,
//                                          dobErrorKey             = "dob_error",
//                                          previousPinErrorKey     = "previous_pin_error",
//                                          controllerComponents    = stubControllerComponents())
//
//  def mockMongoService(response: Future[List[String]])(implicit mongoService: MongoService) =
//    when(mongoService.getLastThreePin(any())(any()))
//      .thenReturn(response)
//
//  "ValidateControllerSpec" should {
//
//    "return 200 " when {
//
//      "valid pin is entered while pin creation" in {
//        mockAuthorisationGrantAccess(grantAccessWithCL200)
//        when(mockCustomerProfileService.getNino()).thenReturn(Future.successful(Some(nino)))
//        when(requestBuilderExecute[Option[PersonDetails]])
//          .thenReturn(Future.successful(Some(person4)))
//        // when(mockCitizenDetailsConnector.personDetailsForPin(nino)).thenReturn(Future.successful(Some(person4)))
//        val result = controller.validatePin("300684", deviceId, journeyId, "createPin")(fakeRequest)
//        status(result) mustBe (200)
//
//      }
//
////      "valid pin is entered while pin update" in {
////        mockAuthorisationGrantAccess(grantAccessWithCL200)
////        mockGetPerson[PersonDetails](Future.successful(createPersonDetails(dob1)))
////        mockMongoService(Future.successful(List(hash1, hash2)))
////        val result = controller.validatePin(nino, "300685", deviceId, journeyId, "updatePin")(fakeRequest)
////        status(result) shouldBe (200)
////
////      }
//
//    }
//
////    "return 401" when {
////      "a pin matching dob is entered" in {
////        mockAuthorisationGrantAccess(grantAccessWithCL200)
////        mockGetPerson[PersonDetails](Future.successful(createPersonDetails(dob1)))
////        val result   = controller.validatePin(nino, "240780", deviceId, journeyId, "createPin")(fakeRequest)
////        val jsonBody = contentAsJson(result)
////        status(result) shouldBe (401)
////        (jsonBody \ "key").as[String] mustBe "create_pin_date_of_birth_error_message"
////      }
////
////      "valid pin is matching last 3 pin is entered for pin update" in {
////        mockAuthorisationGrantAccess(grantAccessWithCL200)
////        mockGetPerson[PersonDetails](Future.successful(createPersonDetails(dob1)))
////        mockMongoService(Future.successful(List(hash1, hash2)))
////        val result   = controller.validatePin(nino, "24072012", deviceId, journeyId, "updatePin")(fakeRequest)
////        val jsonBody = contentAsJson(result)
////        status(result) shouldBe (401)
////        (jsonBody \ "key").as[String] mustBe "change_pin_disallow_previous_pins_error_message"
////
////      }
////    }
//  }
//
//}
