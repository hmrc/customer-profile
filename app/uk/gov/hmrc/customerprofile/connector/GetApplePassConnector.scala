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
import uk.gov.hmrc.customerprofile.domain.{ApplePass, GetApplePass, ApplePassUUIDGenerator}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, CoreGet, CorePost, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class GetApplePassConnector @Inject()(httpPost: CorePost, httpGet : CoreGet, @Named("find-my-nino-add-to-wallet") findMyNinoAddToWalletUrl: String){

  val logger: Logger = Logger(this.getClass)

  def createApplePass(nino : Nino, name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GetApplePass]  = {
    httpPost.POST[ApplePassUUIDGenerator,GetApplePass](url = s"$findMyNinoAddToWalletUrl/create-apple-pass", ApplePassUUIDGenerator(name, nino)) recover {
      case e => {
        logger.info(s"Error: ${e.getMessage}")
        throw e
      }
    }
  }

  def getPass(passId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[ApplePass] = {
    httpGet.GET[ApplePass](url = s"$findMyNinoAddToWalletUrl/get-pass-card?=$passId") recover {
      case e => {
        logger.info(s"Error: ${e.getMessage}")
        throw e
      }
    }
  }
}
