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

package uk.gov.hmrc.customerprofile.connector

import org.mockito.Mockito.when
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain.{Paperless, PaperlessOptOut, Preference, TermsAccepted}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, NotFoundException, UpstreamErrorResponse}
import scala.concurrent.Future

import scala.util.{Failure, Success}

class EntityResolverConnectorSpec extends HttpClientV2Helper with MockFactory {

  val baseUrl: String = "http://entity-resolver.service"
  val termsAndConditionsPostUrl: String = s"$baseUrl/preferences/terms-and-conditions"
  val circuitBreakerNumberOfCallsToTriggerStateChange: Int = 5

  // create a new connector each time because the circuit breaker is stateful
  def entityResolverConnector: EntityResolverConnector = {
    def mockCircuitBreakerConfig() = {

      when(config.getOptional[Configuration]("microservice.services.entity-resolver")).thenReturn(Some(config))
      when(config.getOptional[Int]("circuitBreaker.numberOfCallsToTriggerStateChange"))
        .thenReturn(Some(circuitBreakerNumberOfCallsToTriggerStateChange))
      when(config.getOptional[Int]("circuitBreaker.unavailablePeriodDurationInSeconds")).thenReturn(Some(2000))
      when(config.getOptional[Int]("circuitBreaker.unstablePeriodDurationInSeconds")).thenReturn(Some(2000))

    }

    mockCircuitBreakerConfig()
    new EntityResolverConnector(baseUrl, mockHttpClient, config, environment)
  }

  "getPreferences()" should {
    val preferences = Some(Preference(digital = true))
    "return the preferences for utr only" in {
      when(requestBuilderExecute[Option[Preference]]).thenReturn(Future.successful(preferences))

      entityResolverConnector.getPreferences() onComplete {
        case Success(_) => preferences
        case Failure(_) =>
      }
    }

    "return the preferences with linkSent daye when email is pending" in {

      when(requestBuilderExecute[Option[Preference]]).thenReturn(Future.successful(preferences))
      entityResolverConnector.getPreferences() onComplete {
        case Success(_) => preferences
        case Failure(_) =>
      }
    }

    "return None for a 404" in {

      when(requestBuilderExecute[Option[Preference]]).thenReturn(Future.failed(new NotFoundException("where are you?")))
      entityResolverConnector.getPreferences() onComplete {
        case Success(_) => None
        case Failure(_) =>
      }
    }

    "return None for a 410" in {

      when(requestBuilderExecute[Option[Preference]]).thenReturn(Future.failed(UpstreamErrorResponse("GONE", 410, 410)))
      entityResolverConnector.getPreferences() onComplete {
        case Success(_) => None
        case Failure(_) =>
      }
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {

      1 to circuitBreakerNumberOfCallsToTriggerStateChange foreach { _ =>
        when(requestBuilderExecute[Option[Preference]])
          .thenReturn(Future.failed(new InternalServerException("some exception")))

        entityResolverConnector.getPreferences() onComplete {
          case Success(_) => None
          case Failure(_) =>
        }
      }
    }

    "paperlessSettings()" should {
      val email = EmailAddress("me@mine.com")
      val paperlessSettingsAccepted = Paperless(TermsAccepted(Some(true)), email, Some(English))
      val paperlessSettingsRejected = Paperless(TermsAccepted(Some(false)), email, Some(English))

      "update record to opted in when terms are accepted" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsAccepted) onComplete {
          case Success(_) => PreferencesExists
          case Failure(_) =>
        }
      }

      "update record to opted out when terms are rejected" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsRejected) onComplete {
          case Success(_) => PreferencesExists
          case Failure(_) =>
        }
      }

      "create opt in record when terms are accepted" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(201, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsAccepted) onComplete {
          case Success(_) => PreferencesCreated
          case Failure(_) =>
        }
      }

      "create opt out record when terms are rejected" in {
        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(201, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsRejected) onComplete {
          case Success(_) => PreferencesCreated
          case Failure(_) =>
        }
      }

      "report failure for unexpected response code when terms are accepted" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(204, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsAccepted) onComplete {
          case Success(_) => PreferencesFailure
          case Failure(_) =>
        }
      }

      "report failure for unexpected response code when terms are rejected" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(204, "")))
        entityResolverConnector.paperlessSettings(paperlessSettingsRejected) onComplete {
          case Success(_) => PreferencesFailure
          case Failure(_) =>
        }
      }

      "throw an exception if the call fails" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(UpstreamErrorResponse("error", 500, 500)))
        entityResolverConnector.paperlessSettings(paperlessSettingsRejected) onComplete {
          case Success(_) => fail()
          case Failure(_) =>
        }
      }
    }

    "paperlessOptOut()" should {
      "update record to opted out" in {

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(UpstreamErrorResponse("error", 500, 500)))
        entityResolverConnector.paperlessOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))) onComplete {
          case Success(_) => PreferencesExists
          case Failure(_) =>
        }
      }

    }

    "create opt out record" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(201, "")))
      entityResolverConnector.paperlessOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))) onComplete {
        case Success(_) => PreferencesCreated
        case Failure(_) =>
      }
    }
  }

  "report failure for unexpected response code" in {
    when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(204, "")))
    entityResolverConnector.paperlessOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))) onComplete {
      case Success(_) => PreferencesFailure
      case Failure(_) =>
    }
  }

  "report PreferencesDoesNotExist when not found" in {

    when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(404, "")))
    entityResolverConnector.paperlessOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))) onComplete {
      case Success(_) => PreferencesDoesNotExist
      case Failure(_) =>
    }

  }

  "throw an exception if the call fails" in {

    when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(UpstreamErrorResponse("error", 500, 500)))
    entityResolverConnector.paperlessOptOut(PaperlessOptOut(Some(TermsAccepted(Some(false))), Some(English))) onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }
  }
}
