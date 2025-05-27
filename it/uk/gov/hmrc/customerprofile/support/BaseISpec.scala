/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.support

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino

import eu.timepit.refined.auto._
import java.io.InputStream
import scala.concurrent.Future
import scala.io.Source.fromInputStream

class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {

  override implicit lazy val app: Application = appBuilder
    .build()

  val nino:                    Nino             = Nino("AA000006C")
  val acceptJsonHeader:        (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"
  val authorisationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer 123"
  val journeyId:               JourneyId        = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"

  def getRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorisationJsonHeader).get()

  def postRequestWithAcceptHeader(
    url:  String,
    form: JsValue
  ): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorisationJsonHeader).post(form)

  def postRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorisationJsonHeader).post("")

  def config: Map[String, Any] = Map(
    "auditing.enabled"                                      -> false,
    "microservice.services.auth.port"                       -> wireMockPort,
    "microservice.services.citizen-details.port"            -> wireMockPort,
    "microservice.services.entity-resolver.port"            -> wireMockPort,
    "microservice.services.preferences.port"                -> wireMockPort,
    "play.ws.timeout.connection"                            -> "6000 seconds",
    "play.ws.timeout.request"                               -> "20000 seconds",
    "microservice.services.mobile-shuttering.port"          -> wireMockPort,
    "optInVersionsEnabled"                                  -> false,
    "metrics.jvm.enabled"                                   -> false,
    "microservice.services.find-my-nino-add-to-wallet.port" -> wireMockPort,
    "googlePass.key"                                        -> "ey23xs"
  )

  protected def resourceAsString(resourcePath: String): Option[String] =
    withResourceStream(resourcePath) { is =>
      fromInputStream(is).mkString
    }

  protected def resourceAsJsValue(resourcePath: String): Option[JsValue] =
    withResourceStream(resourcePath) { is =>
      Json.parse(is)
    }

  protected def getResourceAsJsValue(resourcePath: String): JsValue =
    resourceAsJsValue(resourcePath).getOrElse(throw new RuntimeException(s"Could not find resource $resourcePath"))

  protected def withResourceStream[A](resourcePath: String)(f: InputStream => A): Option[A] =
    Option(getClass.getResourceAsStream(resourcePath)) map { is =>
      try {
        f(is)
      } finally {
        is.close()
      }
    }

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
