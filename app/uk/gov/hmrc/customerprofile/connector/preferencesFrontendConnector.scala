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
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.StatusWithUrl
import uk.gov.hmrc.http.{CoreGet, CorePut, HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class preferencesFrontendConnector @Inject() (
  http:                                     CorePut with CoreGet,
  @Named("preferencesFrontend") serviceUrl: String,
  override val externalServiceName:         String,
  val configuration:                        Configuration,
  val environment:                          Environment)
    extends ServicesCircuitBreaker {

  def url(path: String): String = s"$serviceUrl$path"

  def getStatus(
                    )(implicit headerCarrier: HeaderCarrier,
                      ex:                     ExecutionContext
                    ): Future[Option[StatusWithUrl]] =
    withCircuitBreaker(http.GET[Option[StatusWithUrl]](url(s"/paperless/status")))
      .recover {
        case _:        NotFoundException                                            => None
      }

}
