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

package uk.gov.hmrc.customerprofile.utils

import java.time.LocalDate

object DOBUtils {

  def matchesDOBPatterns(
    dateOfBirth: Option[LocalDate],
    input:       String
  ): Boolean =
    dateOfBirth match {
      case Some(dob) =>
        val year2  = f"${dob.getYear % 100}%02d"
        val year4  = f"${dob.getYear}%02d"
        val month2 = f"${dob.getMonthValue}%02d"
        val month1 = f"${dob.getMonthValue}%01d"
        val day2   = f"${dob.getDayOfMonth}%02d"
        val day1   = f"${dob.getDayOfMonth}%01d"

        val patterns = Seq(
          s"$day2$month2$year2", // DDMMYY
          s"$day2$year2$month2", // DDYYMM
          s"$month2$day2$year2", // MMDDYY
          s"$month2$year2$day2", // MMYYDD
          s"$year2$day2$month2", // YYDDMM
          s"$year2$month2$day2", // YYMMDD
          s"$year4$month2", // YYYYMM
          s"$month2$year4", // MMYYYY
          s"$year4$day2", // YYYYDD
          s"$day2$year4" // DDYYYY
        )
        val patterns2 = if (month1.size == 1 && day1.size == 1) {
          Seq(s"$day1$month1$year4", // DMYYYY
              s"$month1$day1$year4", // MDYYYY
              s"$year4$day1$month1", // YYYYDM
              s"$year4$month1$day1") // YYYYMD
        } else Seq.empty

        (patterns ++ patterns2).contains(input)
      case None => false
    }

}
