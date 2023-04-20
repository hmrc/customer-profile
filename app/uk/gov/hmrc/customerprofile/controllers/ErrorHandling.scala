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

package uk.gov.hmrc.customerprofile.controllers

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Result
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
case object ErrorUnauthorizedNoNino
    extends ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", "NINO does not exist on account")

case object ErrorManualCorrespondenceIndicator
    extends ErrorResponse(LOCKED,
                          "MANUAL_CORRESPONDENCE_IND",
                          "Data cannot be disclosed to the user because MCI flag is set in NPS")

case object ErrorPreferenceConflict extends ErrorResponse(CONFLICT, "CONFLICT", "No existing verified or pending data")

case object ErrorUnauthorizedMicroService
  extends ErrorResponse(401, "UNAUTHORIZED", "Unauthorized to access resource") {

  implicit val writes: Writes[ErrorResponse] = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }
}

class FailToMatchTaxIdOnAuth(message: String) extends HttpException(message, 403)

class NinoNotFoundOnAccount(message: String) extends HttpException(message, 403)

class AccountWithLowCL(message: String) extends HttpException(message, 403)

trait ErrorHandling {
  self: BackendBaseController =>
  val app: String
  val logger: Logger = Logger(this.getClass)

  def log(message: String): Unit = logger.info(s"$app $message")

  def result(errorResponse: ErrorResponse): Result =
    Status(errorResponse.httpStatusCode)(toJson(errorResponse))
  def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    func.recover {
      case e: AuthorisationException =>
        Unauthorized(obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))

      case _: NotFoundException =>
        log("Resource not found!")
        result(ErrorNotFound)

      case _: NinoNotFoundOnAccount =>
        log("User has no NINO. Unauthorized!")
        Forbidden(toJson[ErrorResponse](ErrorUnauthorizedNoNino))

      case e: Throwable =>
        logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson[ErrorResponse](ErrorInternalServerError))
    }
}