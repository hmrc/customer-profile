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

import com.google.inject.Singleton
import play.api.Logger

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.customerprofile.domain.StatusName.{Bounced, Pending, ReOptIn, Verified}
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxCustomerProfileController @Inject() (
  cc:                            ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with CustomerProfileController
    with HeaderValidator
    with FileResource {
  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  override val logger: Logger = Logger(this.getClass)

  private val SANDBOX_CONTROL_HEADER = "SANDBOX-CONTROL"

  private final val WebServerIsDown = new Status(521)

  private def preferencesSandbox(
    status:   StatusName,
    linkSent: Option[org.joda.time.LocalDate] = None
  ) =
    Preference(
      digital      = true,
      emailAddress = Some("jt@test.com"),
      status =
        if (status == ReOptIn) Some(PaperlessStatus(status, Category.ActionRequired, Some(10)))
        else Some(PaperlessStatus(status, Category.ActionRequired)),
      linkSent = linkSent
    )

  override def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request, AnyContent] =
    validateAccept(acceptHeaderValidationRules)

  override def withShuttering(shuttering: Shuttering)(fn: => Future[Result]): Future[Result] =
    if (shuttering.shuttered)
      Future.successful(WebServerIsDown(Json.toJson(shuttering)))
    else fn

  override def getPersonalDetails(
    nino:      Nino,
    journeyId: JourneyId
  ): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
        case Some("ERROR-401") => Unauthorized
        case Some("ERROR-403") => Forbidden
        case Some("ERROR-500") => InternalServerError
        case _ =>
          Ok(
            findResource(s"/sandbox/personal-details.json")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
          )
      })
    }

  override def getPreferences(journeyId: JourneyId): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
        case Some("ERROR-401") => Unauthorized
        case Some("ERROR-403") => Forbidden
        case Some("ERROR-404") => NotFound
        case Some("ERROR-500") => InternalServerError
        case Some("VERIFIED") =>
          Ok(
            toJson(
              preferencesSandbox(Verified)
            )
          )
        case Some("UNVERIFIED") =>
          Ok(
            toJson(
              preferencesSandbox(Pending, Some(org.joda.time.LocalDate.now()))
            )
          )
        case Some("BOUNCED") =>
          Ok(
            toJson(
              preferencesSandbox(Bounced)
            )
          )
        case Some("REOPTIN") =>
          Ok(
            toJson(
              preferencesSandbox(ReOptIn)
            )
          )
        case _ =>
          Ok(
            toJson(
              Preference(digital = false, None, None, None)
            )
          )
      })
    }

  override def optOut(
    settings:    PaperlessOptOut,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    request:     Request[_]
  ): Future[Result] =
    Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
      case Some("ERROR-401")          => Unauthorized
      case Some("ERROR-403")          => Forbidden
      case Some("ERROR-404")          => NotFound
      case Some("ERROR-500")          => InternalServerError
      case Some("PREFERENCE-CREATED") => Created
      case _                          => NoContent
    })

  override def optIn(
    settings:    Paperless,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    request:     Request[_]
  ): Future[Result] =
    Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
      case Some("ERROR-401")          => Unauthorized
      case Some("ERROR-403")          => Forbidden
      case Some("ERROR-404")          => NotFound
      case Some("ERROR-409")          => Conflict
      case Some("ERROR-500")          => InternalServerError
      case Some("PREFERENCE-CREATED") => Created
      case _                          => NoContent
    })

  override def pendingEmail(
    changeEmail: ChangeEmail,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    request:     Request[_]
  ): Future[Result] =
    Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
      case Some("ERROR-401") => Unauthorized
      case Some("ERROR-403") => Forbidden
      case Some("ERROR-404") => NotFound
      case Some("ERROR-409") => Conflict
      case Some("ERROR-500") => InternalServerError
      case _                 => NoContent
    })

}
