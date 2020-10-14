/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import javax.inject.Named
import play.api.{Configuration, Environment}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.StatusWithUrl
import uk.gov.hmrc.http.{CoreGet, CorePut, HeaderCarrier, NotFoundException}
import views.html.helper.urlEncode

import scala.concurrent.{ExecutionContext, Future}

class PreferencesFrontendConnector @Inject() (
  http:                                      CorePut with CoreGet,
  @Named("preferences-frontend") serviceUrl: String,
  val configuration:                         Configuration,
  val environment:                           Environment,
  applicationCrypto:                         ApplicationCrypto)
    extends ServicesCircuitBreaker {

  val externalServiceName: String = "preferences-frontend"

  def url(path: String): String =
    s"$serviceUrl$path" +
    s"?returnUrl=${encryptAndEncode("returnUrl")}" +
    s"&returnLinkText=${encryptAndEncode("Continue")}"

  def getStatus(
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[StatusWithUrl]] =
    withCircuitBreaker(http.GET[Option[StatusWithUrl]](url(s"/paperless/status")))
      .recover {
        case _: NotFoundException => None
      }

  private def encryptAndEncode(s: String): String =
    urlEncode(applicationCrypto.QueryParameterCrypto.encrypt(PlainText(s)).value)

}
