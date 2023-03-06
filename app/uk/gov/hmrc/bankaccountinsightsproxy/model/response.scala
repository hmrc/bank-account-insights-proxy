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

import play.api.libs.json.{Format, Json}

import java.time.LocalDateTime

/** riskScore: 0-100 <br/> reason:
  * ACCOUNT_NOT_ON_WATCH_LIST|ACCOUNT_ON_WATCH_LIST
  */
object response {
  object risklist_response_codes {
    val ACCOUNT_ON_WATCH_LIST: String = "ACCOUNT_ON_WATCH_LIST"
    val ACCOUNT_NOT_ON_WATCH_LIST: String = "ACCOUNT_NOT_ON_WATCH_LIST"
  }
  final case class BankAccountInsightsResponse(
      bankAccountInsightsCorrelationId: String,
      riskScore: Int,
      reason: String
  )
  object BankAccountInsightsResponse {
    object implicits {
      implicit val bankAccountInsightsResponseFormat
          : Format[BankAccountInsightsResponse] =
        Json.format[BankAccountInsightsResponse]
    }
  }

  final case class IPPResponseComponentValue(value: String, numOfOccurrences: Int, lastSeen: LocalDateTime)
  object IPPResponseComponentValue {
    object Implicits {
      implicit val ippResponseComponentValueFormat: Format[IPPResponseComponentValue] = Json.format[IPPResponseComponentValue]
    }
  }

  final case class IPPResponseComponent(identifier: String, count: Int, componentValues: List[IPPResponseComponentValue])

  object IPPResponseComponent {
    object Implicits {
      import IPPResponseComponentValue.Implicits._
      implicit val ippResponseComponentFormat: Format[IPPResponseComponent] = Json.format[IPPResponseComponent]
    }
  }

  final case class IPPResponse(sortCode: String, accountNumber: String, correlationId: Option[String], ippComponents: List[IPPResponseComponent])

  object IPPResponse {
    object Implicits {
      import IPPResponseComponent.Implicits._
      implicit val ippResponseFormat: Format[IPPResponse] = Json.format[IPPResponse]
    }
  }

}
