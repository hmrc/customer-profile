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

package uk.gov.hmrc.customerprofile.utils

import com.google.auth.oauth2.GoogleCredentials

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util.Base64

// $SCOVERAGE-OFF$

object GoogleCredentialsSerializer {

  def serializeToBase64String(credentials: GoogleCredentials): String = {
    val outputStream = new ByteArrayOutputStream()
    val objectOutputStream = new ObjectOutputStream(outputStream)
    objectOutputStream.writeObject(credentials)
    objectOutputStream.flush()
    Base64.getEncoder.encodeToString(outputStream.toByteArray)
  }
}
// $COVERAGE-ON$
