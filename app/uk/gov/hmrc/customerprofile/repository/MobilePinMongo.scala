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

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.Logger
import org.mongodb.scala.model.Filters.*
import uk.gov.hmrc.customerprofile.domain.{MobilePin, ServiceResponse}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.model.Indexes.{ascending, descending}
import uk.gov.hmrc.customerprofile.config.AppConfig
import uk.gov.hmrc.customerprofile.errors.MongoDBError
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MobilePinMongo @Inject() (
  mongo: MongoComponent,
  appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[MobilePin](
      collectionName = "mobilePin",
      mongoComponent = mongo,
      domainFormat   = MobilePin.format,
      indexes = Seq(
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("deviceId"),
            Indexes.ascending("ninoHash")
          ),
          IndexOptions()
            .name("device-nino-index")
            .unique(true)
            .background(true)
        ),
        IndexModel(ascending("createdAt"),
                   IndexOptions()
                     .background(false)
                     .name("createdAt")
                  ),
        IndexModel(descending("updatedAt"),
                   IndexOptions()
                     .background(false)
                     .name("updatedAt")
                     .expireAfter(appConfig.mongoTtl, TimeUnit.DAYS)
                  )
      ),
      replaceIndexes = true
    ) {

  val logger: Logger = Logger(this.getClass)

  def add(mobilePin: MobilePin): ServiceResponse[MobilePin] = {
    val currentTime = Instant.now()
    collection
      .insertOne(mobilePin.copy(updatedAt = Some(currentTime), createdAt = Some(currentTime)))
      .toFuture()
      .map(_ => Right(mobilePin))
      .recover { case _ =>
        Left(MongoDBError("Unexpected error while writing a document."))
      }
  }

  def update(updatedMobilePin: MobilePin): ServiceResponse[MobilePin] = {

    val filter = and(equal("deviceId", updatedMobilePin.deviceId), equal("ninoHash", updatedMobilePin.ninoHash))
    val currentTime = Instant.now()
    collection
      .replaceOne(filter, updatedMobilePin.copy(updatedAt = Some(Instant.now)), ReplaceOptions().upsert(true))
      .toFuture()
      .map(_ => Right(updatedMobilePin))
      .recover { case _ =>
        Left(MongoDBError("Unexpected error while editing a document."))
      }
  }

  def findByDeviceIdAndNino(
    deviceId: String,
    ninoHash: String
  ): ServiceResponse[Option[MobilePin]] = {

    val filter = and(equal("deviceId", deviceId), equal("ninoHash", ninoHash))
    collection
      .find[MobilePin](filter)
      .toFuture()
      .map(data => Right(data.headOption))
      .recover { case _ =>
        Left(MongoDBError("Unexpected error while searching  a document."))
      }
  }

  def deleteAll: ServiceResponse[Unit] =
    collection
      .drop()
      .toFuture()
      .map(_ => Right(()))
      .recover { case _ =>
        Left(MongoDBError("Unexpected error while searching  a document."))

      }

  def deleteOne(
    deviceId: String,
    ninoHash: String
  ): ServiceResponse[Unit] = {
    val filter = and(equal("deviceId", deviceId), equal("ninoHash", ninoHash))
    collection
      .deleteOne(filter)
      .toFuture()
      .map(_ => Right(()))
      .recover { case _ =>
        Left(MongoDBError("Unexpected error while searching  a document."))

      }
  }
}
