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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customerprofile.domain.{ApplePassUUIDGenerator, GetApplePass, RetrieveApplePass}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpClient, HttpException, HttpResponse}

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class ApplePassConnector @Inject()(http: HttpClient, httpGet : CoreGet, @Named("find-my-nino-add-to-wallet") findMyNinoAddToWalletUrl: String){

  val logger: Logger = Logger(this.getClass)
  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")

//  def createApplePass(nino : Nino, name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GetApplePass]  = {
//    httpPost.POST[ApplePassUUIDGenerator,GetApplePass](url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/create-apple-pass", ApplePassUUIDGenerator(name, nino)) recover {
//      case e =>
//        logger.info(s"Error: ${e.getMessage}")
//        throw e
//    }
//  }

  def createApplePass(nino: Nino, fullName: String)
                     (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Some[String]] = {

    val url = s"${findMyNinoAddToWalletUrl}/find-my-nino-add-to-wallet/create-apple-pass"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val details = ApplePassUUIDGenerator(fullName, nino)

    http.POST[JsValue, HttpResponse](url, Json.toJson(details))
      .map { response =>
        println("Calling this service" + response.body)
        response.status match {
          case OK => Some(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

//  def getPass(passId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[RetrieveApplePass] = {
//    httpGet.GET[RetrieveApplePass](url = s"$findMyNinoAddToWalletUrl/find-my-nino-add-to-wallet/get-pass-card?=$passId") recover {
//      case e =>
//        logger.info(s"Error: ${e.getMessage}")
//        throw e
//    }
//  }

}
