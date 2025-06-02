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

package uk.gov.hmrc.customerprofile.repository

import uk.gov.hmrc.customerprofile.domain.MobilePin
import uk.gov.hmrc.customerprofile.errors.MongoDBError
import uk.gov.hmrc.customerprofile.utils.{BaseSpec, PlayMongoRepositorySupport}

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MobilePinMongoSpec extends BaseSpec with PlayMongoRepositorySupport[MobilePin] {

  override lazy val repository = new MobilePinMongo(mongoComponent, appConfig)
  val uuid                     = UUID.randomUUID().toString
  val uuid2                    = UUID.randomUUID().toString
  val dateTime                 = LocalDateTime.of(2025, 4, 4, 0, 0) // 4 April 2025 at 00:00
  val instant                  = dateTime.toInstant(ZoneOffset.UTC)

  "add a new record" should {

    "record is  inserted properly" in {
      repository.collection.drop()
      val mobilePin = MobilePin(uuid, List("123", "345", "678"))
      repository.add(mobilePin).futureValue
      val countF: Future[Long] = repository.collection.countDocuments().toFuture()
      val count:  Long         = await(countF)
      count mustBe  1
    }

    "record is not inserted properly" in {
      repository.collection.drop()
      val mobilePin1 = MobilePin(uuid, List("123", "345", "678"))
      repository.add(mobilePin1).futureValue
      val mobilePin2 = MobilePin(uuid, List("12311", "345", "678"))
      val result     = repository.add(mobilePin2).futureValue
      result mustBe (Left(MongoDBError("Unexpected error while writing a document.")))
    }

  }

  "update function " should {

    "update the record is already exists" in {
      repository.collection.drop()

      val mobilePin = MobilePin(uuid, List("123", "345", "678"), createdAt = Some(instant))
      val updatedPin = mobilePin
        .copy(hashedPins = List("345", "678", "918"))
      repository.add(mobilePin).futureValue
      repository.update(updatedPin).futureValue
      val record        = repository.findByDeviceId(uuid).futureValue
      val updatedRecord = record.toOption.get
      updatedRecord.get.hashedPins.takeRight(1).head mustBe "918"
      updatedRecord.get.updatedAt.isDefined          mustBe (true)
    }

    "add the record if not existing already" in {
      repository.collection.drop()

      val mobilePin = MobilePin(uuid, List("123", "345", "678"))
      repository.update(mobilePin).futureValue
      val record = repository.findByDeviceId(uuid).futureValue.toOption.get.get

      record.deviceId            mustBe (uuid)
      record.hashedPins          mustBe (List("123", "345", "678"))
      record.updatedAt.isDefined mustBe (true)

    }

  }

  "find the record " should {

    "give a record response if exists" in {
      repository.collection.drop()

      val mobilePin = MobilePin(uuid, List("12345", "345"))
      repository.add(mobilePin).futureValue
      val record = repository.findByDeviceId(uuid).futureValue.toOption.get.get

      record.deviceId            mustBe (uuid)
      record.hashedPins          mustBe (List("12345", "345"))
      record.createdAt.isDefined mustBe (true)
      record.updatedAt.isDefined mustBe (true)
    }

    "give a None response if record don't exists" in {
      repository.collection.drop()

      val record: Option[MobilePin] = repository.findByDeviceId(uuid).futureValue.toOption.get
      record.isDefined mustBe (false)

    }

  }

  "delete all records " should {

    "delete all records from DB" in {
      repository.collection.drop()
      val mobilePin = MobilePin(uuid, List("123", "345", "678"), createdAt = Some(instant))
      repository.add(mobilePin).futureValue
      repository.deleteAll.futureValue
      val count = repository.collection.countDocuments().toFuture()
      count onComplete {
        case Success(counter) => counter mustBe (0)
        case Failure(_)       =>
      }

    }
  }

  "delete all records " should {

    "delete the records from DB by DeviceId" in {
      repository.collection.drop()
      val mobilePin1 = MobilePin(uuid, List("123", "345", "678"), createdAt    = Some(instant))
      val mobilePin2 = MobilePin(uuid2, List("311", "2134", "5467"), createdAt = Some(instant))
      repository.add(mobilePin1).futureValue
      repository.add(mobilePin2).futureValue
      repository.deleteOne(uuid).futureValue
      val count = repository.collection.countDocuments().toFuture()
      count onComplete {
        case Success(counter) => counter mustBe (1)
        case Failure(_)       =>
      }

    }
  }

}
