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

import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customerprofile.domain.{ApplePassIdGenerator, RetrieveApplePass}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class ApplePassConnector @Inject()(http: HttpClient,  @Named("find-my-nino-add-to-wallet") findMyNinoAddToWalletUrl: String){

  val logger: Logger = Logger(this.getClass)
  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")

  def createApplePass(nino: Nino, fullName: String)
                     (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Some[String]] = {

    val url = s"${findMyNinoAddToWalletUrl}/find-my-nino-add-to-wallet/create-apple-pass"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val details = ApplePassIdGenerator(fullName, nino)

    http.POST[JsValue, HttpResponse](url, Json.toJson(details))
      .map { response =>
        response.status match {
          case OK => Some(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getApplePass(passId: String)
                  (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[RetrieveApplePass] = {

    val url = s"${findMyNinoAddToWalletUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](url)
      .map { response =>
        response.status match {
          case OK => RetrieveApplePass(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }



}
