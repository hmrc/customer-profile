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

import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.{ApplePassIdGenerator, RetrieveApplePass}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class ApplePassConnector @Inject() (
                                     http: HttpClientV2,
  @Named("find-my-nino-add-to-wallet") findMyNinoAddToWalletUrl: String) {

  val logger: Logger = Logger(this.getClass)

  def createApplePass(
    nino:          String,
    fullName:      String
  )(implicit ec:   ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[String] = {

    val url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/create-apple-pass"

    val details = ApplePassIdGenerator(fullName, nino)

    http.post(url"$url")
      .withBody(Json.toJson(details))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => response.body
          case _  => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getApplePass(
    passId:        String
  )(implicit ec:   ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[RetrieveApplePass] = {

    val url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"


    http.get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => RetrieveApplePass(response.body)
          case _  => throw new HttpException(response.body, response.status)
        }
      }
  }

}
