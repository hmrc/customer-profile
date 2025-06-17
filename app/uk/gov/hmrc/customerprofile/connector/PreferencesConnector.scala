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
import play.api.http.Status.OK

import javax.inject.Named
import play.api.libs.json.{Json, OFormat}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

case class Entity(_id: String)

object Entity {
  implicit val format: OFormat[Entity] = Json.format[Entity]
}

@Singleton
class PreferencesConnector @Inject() (http: HttpClientV2,
                                      @Named("preferences") serviceUrl: String,
                                      override val externalServiceName: String,
                                      val configuration: Configuration,
                                      val environment: Environment
                                     )
    extends ServicesCircuitBreaker {

  val logger: Logger = Logger(this.getClass)
  val NOT_FOUND = 404
  val CONFLICT = 409

  def url(path: String): String = s"$serviceUrl$path"

  def updatePendingEmail(
    changeEmail: ChangeEmail,
    entityId: String
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    http
      .put(url"${url(s"/preferences/$entityId/pending-email")}")
      .withBody(Json.toJson(changeEmail))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => EmailUpdateOk
          case NOT_FOUND =>
            log(response.body, entityId)
            NoPreferenceExists
          case CONFLICT =>
            log(response.body, entityId)
            EmailNotExist
          case _ =>
            log("Failed to update preferences email", entityId)
            EmailUpdateFailed
        }
      }

  def log(
    msg: String,
    entityId: String
  ): Unit =
    logger.warn(msg + s" for entity $entityId")
}
