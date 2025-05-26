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

import com.google.inject.name.Named
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Logger
import org.mongodb.scala.model.Filters.equal
import uk.gov.hmrc.customerprofile.domain.{MobilePin, ServiceResponse}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.model.Indexes.{ascending, descending}
import uk.gov.hmrc.customerprofile.errors.MongoDBError

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}

@Singleton
class MobilePinMongo @Inject() (
  mongo:                              MongoComponent,
  @Named("mongodb.ttlDays") mongoTtl: Int
)(implicit executionContext:          ExecutionContext)
    extends PlayMongoRepository[MobilePin](collectionName = "mobilePin",
                                           mongoComponent = mongo,
                                           domainFormat   = MobilePin.format,
                                           indexes = Seq(
                                             IndexModel(ascending("deviceId"),
                                                        IndexOptions()
                                                          .name("deviceId-index")
                                                          .unique(true)
                                                          .background(true)),
                                             IndexModel(ascending("createdAt"),
                                                        IndexOptions()
                                                          .background(false)
                                                          .name("createdAt")
                                                          .expireAfter(mongoTtl, TimeUnit.DAYS)),
                                             IndexModel(descending("updatedAt"),
                                                        IndexOptions()
                                                          .background(false)
                                                          .name("updatedAt")
                                                          .expireAfter(mongoTtl, TimeUnit.DAYS))
                                           ),
                                           replaceIndexes = true) {

  val logger: Logger = Logger(this.getClass)

  def add(mobilePin: MobilePin): ServiceResponse[MobilePin] = {
    val currentTime = Instant.now()
    collection
      .insertOne(mobilePin.copy(updatedAt = Some(currentTime), createdAt = Some(currentTime)))
      .toFuture()
      .map(_ => Right(mobilePin))
      .recover {
        case _ => Left(MongoDBError("Unexpected error while writing a document."))
      }
  }

  def update(updatedMobilePin: MobilePin): ServiceResponse[MobilePin] = {

    val filter      = equal("deviceId", updatedMobilePin.deviceId)
    val currentTime = Instant.now()
    collection
      .replaceOne(filter, updatedMobilePin.copy(updatedAt = Some(currentTime)), ReplaceOptions().upsert(true))
      .toFuture()
      .map(_ => Right(updatedMobilePin))
      .recover {
        case _ => Left(MongoDBError("Unexpected error while editing a document."))
      }
  }

  def findByDeviceId(deviceId: String): ServiceResponse[Option[MobilePin]] =
    collection
      .find[MobilePin](equal("deviceId", deviceId))
      .toFuture()
      .map(data => Right(data.headOption))
      .recover {
        case _ => Left(MongoDBError("Unexpected error while searching  a document."))
      }

  def deleteAll: ServiceResponse[Unit] =
    collection
      .drop()
      .toFuture()
      .map(_ => Right())
      .recover {
        case _ => Left(MongoDBError("Unexpected error while searching  a document."))

      }

  def deleteOne(deviceId: String): ServiceResponse[Unit] =
    collection
      .deleteOne(equal("deviceId", deviceId))
      .toFuture()
      .map(_ => Right())
      .recover {
        case _ => Left(MongoDBError("Unexpected error while searching  a document."))

      }
}
