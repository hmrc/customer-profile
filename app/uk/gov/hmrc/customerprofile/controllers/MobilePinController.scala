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

package uk.gov.hmrc.customerprofile.controllers

import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.auth.AccessControl
import uk.gov.hmrc.customerprofile.domain.MobilePinValidatedRequest
import uk.gov.hmrc.customerprofile.domain.audit.MobilePinAudit
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, MobilePinService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class MobilePinController @Inject() (
  override val authConnector:                                   AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  controllerComponents:                                         ControllerComponents,
  val auditConnector:                                           AuditConnector,
  pinService:                                                   MobilePinService,
  customerProfileService:                                       CustomerProfileService
)(implicit val executionContext:                                ExecutionContext)
    extends BackendController(controllerComponents)
    with ErrorHandling
    with AccessControl
    with HeaderValidator {
  override val app:    String                 = "mobile-pin-security"
  override val logger: Logger                 = Logger(this.getClass)
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  def upsert : Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules, None).async(parse.json) { implicit request =>
      implicit val hc: HeaderCarrier = fromRequest(request)
      customerProfileService.getNino().flatMap { nino =>
        request.body
          .validate[MobilePinValidatedRequest]
          .fold(
            errors => {
              logger.warn(s"Invalid request body: $errors")
              Future.successful(
                BadRequest(Json.obj("error" -> "Invalid request format", "details" -> JsError.toJson(errors)))
              )
            },
            validRequest =>
              pinService
                .upsertPin(validRequest, nino)
                .andThen {
                  case Success(_) =>
                    sendAuditEvent(nino, MobilePinAudit.fromResponse(validRequest), request.path)
                }
                .map(_ => Created)
          )
      }
    }

  private def sendAuditEvent(
    nino:        Option[Nino],
    response:    MobilePinAudit,
    path:        String
  )(implicit hc: HeaderCarrier
  ): Unit = auditConnector.sendEvent(
    DataEvent(
      app,
      "Pin Updated/Inserted",
      tags   = hc.toAuditTags("Pin-Updated-Inserted", path),
      detail = Map("nino" -> Future.successful(Some(nino)).toString, "data" -> response.toString)
    )
  )
}
