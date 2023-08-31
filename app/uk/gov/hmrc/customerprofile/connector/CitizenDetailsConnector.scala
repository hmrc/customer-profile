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

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpException, NotFoundException, Upstream4xxResponse, UpstreamErrorResponse}
import play.api.http.Status.{LOCKED, NOT_FOUND}
import uk.gov.hmrc.customerprofile.domain.PersonDetails
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject() (
  @Named("citizen-details") citizenDetailsConnectorUrl: String,
  http:                                                 CoreGet) {

  val logger: Logger = Logger(this.getClass)

  def personDetails(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[PersonDetails] =
    http.GET[PersonDetails](s"$citizenDetailsConnectorUrl/citizen-details/$nino/designatory-details") recover {
      case e: UpstreamErrorResponse if e.statusCode == LOCKED =>
        logger.info("Person details are hidden")
        throw new HttpException(e.getMessage(), LOCKED)
      case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
        logger.info(s"No details found for nino '$nino'")
        throw new HttpException(e.getMessage(), NOT_FOUND)
    }
}
