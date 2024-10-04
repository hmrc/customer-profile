/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.utils

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.customerprofile.connector.HttpClientV2Helper
import uk.gov.hmrc.customerprofile.domain.Shuttering

import scala.concurrent.Future

trait AuthAndShutterMock extends HttpClientV2Helper {

  def mockAuthorisationGrantAccess(response: GrantAccess) =
    when(mockAuthConnector.authorise[GrantAccess](any(), any())(any(), any()))
      .thenReturn(Future.successful(response))

  def mockAuthorisationGrantAccessFail(exception: Exception) =
    when(mockAuthConnector.authorise[GrantAccess](any(), any())(any(), any()))
      .thenReturn(Future.failed(exception))

  def mockShutteringResponse(response: Shuttering) =
    when(requestBuilderExecute[Shuttering])
      .thenReturn(Future.successful(response))

  def mockAuthAccessAndNotShuttered() = {
    mockAuthorisationGrantAccess(grantAccessWithCL200)
    mockShutteringResponse(notShuttered)
  }

  def mockAuthAccessAndShuttered() = {
    mockAuthorisationGrantAccess(grantAccessWithCL200)
    mockShutteringResponse(shuttered)
  }

}
