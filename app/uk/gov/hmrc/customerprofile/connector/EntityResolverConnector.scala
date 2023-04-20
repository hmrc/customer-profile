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

package uk.gov.hmrc.customerprofile.connector

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import play.api.http.Status
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.{Paperless, PaperlessOptOut, Preference}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

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
  http:                                 CoreGet with CorePost,
  val configuration:                    Configuration,
  val environment:                      Environment)
    extends ServicesCircuitBreaker
    with Status {

  import Paperless.formats

  val externalServiceName = "entity-resolver"
  val logger: Logger = Logger(this.getClass)

  def url(path: String) = s"$serviceUrl$path"

  def getPreferences(
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[Preference]] =
    withCircuitBreaker(http.GET[Option[Preference]](url(s"/preferences")))
      .recover {
        case response: UpstreamErrorResponse if response.statusCode == GONE => None
        case _:        NotFoundException                                            => None
      }

  def paperlessSettings(
    paperless:   Paperless
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PreferencesStatus] =
    withCircuitBreaker(http.POST(url(s"/preferences/terms-and-conditions"), paperless)).map(_.status).map {
      case OK      => PreferencesExists
      case CREATED => PreferencesCreated
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
        .POST(url(s"/preferences/terms-and-conditions"), paperlessOptOut)
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
      http.GET[Entity](url(s"/entity-resolver/paye/${nino.nino}"))
    }
}
