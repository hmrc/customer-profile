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
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps}
import com.google.inject.name.Named
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.{GooglePassDetailsWithCredentials, RetrieveGooglePass}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

class GooglePassConnector @Inject() (http: HttpClientV2, @Named("find-my-nino-add-to-wallet") findMyNinoAddToWalletUrl: String) {

  def createGooglePassWithCredentials(
    fullName: String,
    nino: String,
    credentials: String
  )(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[String] = {

    val url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/create-google-pass-with-credentials"

    val details = GooglePassDetailsWithCredentials(fullName, nino, credentials)

    http
      .post(url"$url")
      .withBody(Json.toJson(details))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => response.body
          case _  => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getGooglePassUrl(
    passId: String
  )(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[RetrieveGooglePass] = {

    val url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId"

    http
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            RetrieveGooglePass(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

}
