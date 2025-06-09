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

import jakarta.inject.Singleton
import play.api.Logging
import uk.gov.hmrc.customerprofile.domain.{MobilePin, MobilePinValidatedRequest}
import uk.gov.hmrc.customerprofile.repository.MobilePinMongo
import uk.gov.hmrc.customerprofile.utils.HashSaltUtils
import uk.gov.hmrc.domain.Nino

import java.time.Instant
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MobilePinService @Inject() (
  mobilePinMongo:                                    MobilePinMongo,
  @Named("service.maxStoredPins") val maxStoredPins: Int
)(ec:                                                ExecutionContext)
    extends Logging {

  def upsertPin(
    request:     MobilePinValidatedRequest,
    nino:        Option[Nino]
  )(implicit ec: ExecutionContext
  ): Future[Unit] = {
    val hashedPin = HashSaltUtils.createHashAndSalt(request.pin)
    val now       = Instant.now()
    val hashNino  = HashSaltUtils.createNINOHash(Future.successful(Some(nino)).toString)

    mobilePinMongo.findByDeviceIdAndNino(request.deviceId, hashNino).flatMap {
      case Left(error) =>
        logger.error(s"Failed to fetch device: ${request.deviceId} and the nino combination. Reason: ${error.message}")
        Future.failed(new Exception(error.message))

      case Right(None) =>
        logger.info(s"DeviceId ${request.deviceId} and Nino not found, inserting new entry.")
        val newRecord = MobilePin(
          deviceId   = request.deviceId,
          ninoHash   = hashNino,
          hashedPins = List(hashedPin),
          createdAt  = Some(now),
          updatedAt  = Some(now)
        )
        mobilePinMongo.add(newRecord).map {
          case Left(error) =>
            logger.error(s"Insert failed for ${request.deviceId}: ${error.message}")
            throw new Exception(error.message)
          case Right(_) =>
            logger.info(s"Inserted new pin entry for ${request.deviceId}")
        }

      case Right(Some(existingRecord)) =>
        logger.info(s"updating pin list for existing deviceId: ${request.deviceId}")
        val trimmedPins = existingRecord.hashedPins.takeRight(maxStoredPins - 1)
        val updatedPins = trimmedPins :+ hashedPin

        val updatedRecord = existingRecord.copy(
          hashedPins = updatedPins,
          updatedAt  = Some(now)
        )

        mobilePinMongo.update(updatedRecord).map {
          case Left(error) =>
            logger.error(s"Update failed: ${error.message}")
            throw new Exception(error.message)
          case Right(_) =>
            logger.info(s"Successfully updated deviceId:  ${request.deviceId}")
        }
    }

  }
}
