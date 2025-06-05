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

import org.mockito.Mockito.when
import uk.gov.hmrc.customerprofile.domain.{MobilePin, ServiceResponse}
import uk.gov.hmrc.customerprofile.errors.MongoDBError
import uk.gov.hmrc.customerprofile.repository.MobilePinMongo
import uk.gov.hmrc.customerprofile.utils.BaseSpec

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MongoServiceSpec extends BaseSpec {

  val uuid               = UUID.randomUUID().toString
  val mockMobilePinMongo = mock[MobilePinMongo]

  val service = new MongoService(mockMobilePinMongo)

  def mockFindByDevice(f: ServiceResponse[Option[MobilePin]]) =
    when(
      mockMobilePinMongo
        .findByDeviceIdAndNino(uuid, nino.nino)
    ).thenReturn(f)

  "MongoService" should {

    "return the mobile pin value" in {
      val mobilePin = MobilePin(uuid, nino.nino, List(hash1, hash2, hash3))
      mockFindByDevice(
        Future.successful(
          Right(
            Some(mobilePin)
          )
        )
      )
      service.findByDeviceIdAndNinoHash(uuid, nino.nino) onComplete {
        case Success(value) => value.get mustBe (mobilePin)
        case Failure(_)     => ()
      }
    }

    "return None , if no data is there" in {
      mockFindByDevice(
        Future.successful(
          Right(
            None
          )
        )
      )
      service.findByDeviceIdAndNinoHash(uuid, nino.nino) onComplete {
        case Success(value) => value.isDefined mustBe (false)
        case Failure(_)     => ()
      }
    }

    "return exception if Mongo error occurred while fetching data" in {
      mockFindByDevice(
        Future.successful(
          Left(
            MongoDBError("Mongo Db error")
          )
        )
      )
      service.findByDeviceIdAndNinoHash(uuid, nino.nino) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }
  }
}
