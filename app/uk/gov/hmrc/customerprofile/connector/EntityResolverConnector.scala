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

import com.google.inject.{Inject, Singleton}

import javax.inject.Named
import play.api.http.Status.{CREATED, GONE, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.{Paperless, PaperlessOptOut, Preference}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

sealed trait PreferencesStatus

case object PreferencesExists extends PreferencesStatus

case object PreferencesCreated extends PreferencesStatus

case object PreferencesDoesNotExist extends PreferencesStatus

case object PreferencesFailure extends PreferencesStatus

case object EmailUpdateOk extends PreferencesStatus

case object EmailUpdateFailed extends PreferencesStatus

case object EmailNotExist extends PreferencesStatus

case object NoPreferenceExists extends PreferencesStatus

@Singleton
class EntityResolverConnector @Inject() (
  @Named("entity-resolver") serviceUrl: String,
  http:                                 HttpClientV2,
  val configuration:                    Configuration,
  val environment:                      Environment)
    extends ServicesCircuitBreaker {

  import Paperless.formats

  val externalServiceName = "entity-resolver"
  val logger: Logger = Logger(this.getClass)

  def url(path: String) = s"$serviceUrl$path"

  def getPreferences(
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[Preference]] =
    withCircuitBreaker(http.get(url"${url(s"/preferences")}").execute[Option[Preference]])
      .recover {
        case response: UpstreamErrorResponse if response.statusCode == GONE => None
        case _:        NotFoundException                                    => None
      }

  def paperlessSettings(
    paperless:   Paperless
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PreferencesStatus] =
    withCircuitBreaker(
      http
        .post(url"${url(s"/preferences/terms-and-conditions")}")
        .withBody(Json.toJson(paperless))
        .execute[HttpResponse]
    ).map(_.status).map {
      case OK        => PreferencesExists
      case CREATED   => PreferencesCreated
      case NOT_FOUND => NoPreferenceExists
      case _ =>
        logger.warn("Failed to update paperless settings")
        PreferencesFailure
    }

  def paperlessOptOut(
    paperlessOptOut: PaperlessOptOut
  )(implicit hc:     HeaderCarrier,
    ex:              ExecutionContext
  ): Future[PreferencesStatus] =
    withCircuitBreaker(
      http
        .post(url"${url(s"/preferences/terms-and-conditions")}")
        .withBody(Json.toJson(paperlessOptOut))
        .execute[HttpResponse]
    ).map(_.status)
      .map {
        case OK      => PreferencesExists
        case CREATED =>
          //how could you create an opt-out paperless setting prior to opting-in??
          logger.warn("Unexpected behaviour : creating paperless setting opt-out")
          PreferencesCreated
        case NOT_FOUND =>
          logger.warn("Failed to find a record to apply request to opt-out of paperless settings")
          PreferencesDoesNotExist
        case _ =>
          logger.warn("Failed to apply request to opt-out of paperless settings")
          PreferencesFailure
      }

  def getEntityIdByNino(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Entity] =
    withCircuitBreaker {
      http
        .get(url"${url(s"/entity-resolver/paye/${nino.nino}")}")
        .execute[Entity]
    }

}
