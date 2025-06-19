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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customerprofile.domain.{MobilePin, MobilePinValidatedRequest}
import uk.gov.hmrc.customerprofile.errors.MongoDBError
import uk.gov.hmrc.customerprofile.repository.MobilePinMongo
import uk.gov.hmrc.customerprofile.utils.BaseSpec
import uk.gov.hmrc.domain.Nino

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MobilePinServiceSpec extends BaseSpec with MockitoSugar {

  val mockMongo: MobilePinMongo = mock[MobilePinMongo]
  val service: MobilePinService = new MobilePinService(mockMongo, maxStoredPins = 3)(ec: ExecutionContext)
  val someNino: Option[Nino] = Some(Nino("CS700100A"))

  val deviceId = UUID.randomUUID().toString
  val pin = "828936"
  val dummyPin = MobilePin(deviceId, nino.nino, List("hashed-pin"))
  val request = MobilePinValidatedRequest(pin, deviceId)

  val now: Instant = Instant.now()

  "MobilePinService.upsertPin" should {

    "insert a new record when deviceId does not exist" in {
      when(mockMongo.findByDeviceIdAndNino(any(), any())).thenReturn(Future.successful(Right(None)))
      when(mockMongo.add(any[MobilePin])).thenReturn(Future.successful(Right(dummyPin)))

      service.upsertPin(request, someNino).map { _ =>
        verify(mockMongo).add(
          argThat(record =>
            record.deviceId == deviceId &&
              record.hashedPins.length == 1
          )
        )
        succeed
      }
    }

    "append to existing list if fewer than 3 pins" in {
      val existing = MobilePin(deviceId, nino.nino, List(hash1, hash2), Some(now), Some(now))

      when(mockMongo.findByDeviceIdAndNino(deviceId, nino.nino)).thenReturn(Future.successful(Right(Some(existing))))
      when(mockMongo.update(any[MobilePin])).thenReturn(Future.successful(Right(dummyPin)))

      service.upsertPin(request, someNino).map { _ =>
        verify(mockMongo).update(
          argThat(record =>
            record.hashedPins.length == 3 &&
              record.deviceId == deviceId
          )
        )
        succeed
      }
    }

    "remove oldest pin and append new one when already 3 pins" in {
      val existing = MobilePin(deviceId, nino.nino, List(hash1, hash2, hash3), Some(now), Some(now))

      when(mockMongo.findByDeviceIdAndNino(deviceId, nino.nino)).thenReturn(Future.successful(Right(Some(existing))))
      when(mockMongo.update(any[MobilePin])).thenReturn(Future.successful(Right(dummyPin)))

      service.upsertPin(request, someNino).map { _ =>
        verify(mockMongo).update(
          argThat(record =>
            record.hashedPins.length == 3 &&
              !record.hashedPins.contains(hash1)
          )
        )
        succeed
      }
    }

    "fail if findByDeviceId returns error" in {
      when(mockMongo.findByDeviceIdAndNino(deviceId, nino.nino))
        .thenReturn(Future.successful(Left(MongoDBError("DB error"))))

      recoverToSucceededIf[Exception] {
        service.upsertPin(request, someNino)
      }
    }

    "fail if add returns error" in {
      when(mockMongo.findByDeviceIdAndNino(deviceId, nino.nino)).thenReturn(Future.successful(Right(None)))
      when(mockMongo.add(any[MobilePin])).thenReturn(Future.successful(Left(MongoDBError("Add failed"))))

      recoverToSucceededIf[Exception] {
        service.upsertPin(request, someNino)
      }
    }

    "fail if update returns error" in {
      val existing = MobilePin(deviceId, nino.nino, List(hash1), Some(now), Some(now))

      when(mockMongo.findByDeviceIdAndNino(deviceId, nino.nino)).thenReturn(Future.successful(Right(Some(existing))))
      when(mockMongo.update(any[MobilePin])).thenReturn(Future.successful(Left(MongoDBError("Update failed"))))

      recoverToSucceededIf[Exception] {
        service.upsertPin(request, someNino)
      }
    }
  }
}
