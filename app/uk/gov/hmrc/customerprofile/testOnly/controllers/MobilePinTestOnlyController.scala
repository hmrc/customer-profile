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

package uk.gov.hmrc.customerprofile.testOnly.controllers

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.customerprofile.domain.MobilePin
import uk.gov.hmrc.customerprofile.repository.MobilePinMongo
import uk.gov.hmrc.customerprofile.utils.HashSaltUtils
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MobilePinTestOnlyController @Inject() (
  mobilePinMongo: MobilePinMongo,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def savePin = Action(parse.json).async { implicit request =>
    Json.fromJson[MobilePin](request.body) match {
      case JsSuccess(mobilePin, _) => {

        val hashedPinsInsert = mobilePin.hashedPins.map(x => HashSaltUtils.createHashAndSalt(x))
        val updatedMobilePin =
          mobilePin.copy(ninoHash = HashSaltUtils.createNINOHash(mobilePin.ninoHash), hashedPins = hashedPinsInsert)

        mobilePinMongo.add(updatedMobilePin).map {
          _.fold(
            _ => InternalServerError,
            _ => Created
          )

        }
      }

      case JsError(e) =>
        Future.successful(BadRequest(s"Could not parse JSON: $e"))
    }
  }

  def deleteAllPins = Action.async { implicit request =>
    mobilePinMongo.deleteAll.map {
      _.fold(
        { e =>
          logger.logger.warn(s"Could not delete all tax checks", e)
          InternalServerError
        },
        _ => Ok
      )

    }
  }

  def deleteByDeviceIdAndNino(
    deviceId: String,
    nino: String
  ) = Action.async { implicit request =>
    val hashNino = HashSaltUtils.createNINOHash(nino)
    mobilePinMongo.deleteOne(deviceId, hashNino).map {
      _.fold(
        { e =>
          logger.logger.warn(s"Could not delete mobile pin", e)
          InternalServerError
        },
        _ => Ok
      )

    }
  }

}
