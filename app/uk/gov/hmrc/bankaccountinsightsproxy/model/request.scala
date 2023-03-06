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

package uk.gov.hmrc.bankaccountinsightsproxy.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object request {
  final case class InsightsRequest(
      sortCode: String,
      accountNumber: String
  )
  object InsightsRequest {
    object implicits {
      implicit val bankAccountInsightsRequestReads
          : Reads[InsightsRequest] = (
        (__ \ "sortCode").read(numericSortCode) and
          (__ \ "accountNumber").read(numericAccountNumber)
      )(InsightsRequest.apply _)

      implicit val bankAccountInsightsRequestWrites
          : Writes[InsightsRequest] =
        Json.writes[InsightsRequest]

      private def numericSortCode =
        StringReads
          .filter(JsonValidationError("expected a string with 6 digits")) { s =>
            s.length == 6 && s.forall(_.isDigit)
          }

      private def numericAccountNumber =
        StringReads
          .filter(JsonValidationError("expected a string with 8 digits")) { s =>
            s.length == 8 && s.forall(_.isDigit)
          }
    }
  }
}
