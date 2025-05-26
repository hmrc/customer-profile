/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.services

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.customerprofile.repository.MobilePinMongo

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoService @Inject() (mobilePinMongo: MobilePinMongo) {

  def getLastThreePin(
    deviceId: String
  )(implicit
    ec: ExecutionContext
  ): Future[List[String]] =
    mobilePinMongo.findByDeviceId(deviceId).map { deviceInfo =>
      deviceInfo.fold(
        error => throw new Exception(error.message),
        pins => pins.map(_.hashedPins).getOrElse(List.empty)
      )
    }
}
