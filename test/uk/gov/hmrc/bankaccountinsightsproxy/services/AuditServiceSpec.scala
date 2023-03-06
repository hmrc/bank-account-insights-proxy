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

package uk.gov.hmrc.bankaccountinsightsproxy.services

import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{Headers, Request}
import uk.gov.hmrc.bankaccountinsightsproxy.model.audit.AuditItem
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}

import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends AnyWordSpec with Matchers {
  "AuditService" should {
    "send correct audit information" when {
      "An AuditItem containing a case class is correctly converted to the equivalent Map v3" in {
        val ra = Map(
          "sortCode" -> "123456",
          "accountNumber" -> "12345678",
          "riskScore" -> "100",
          "reason" -> "ACCOUNT_NOT_ON_WATCH_LIST",
          "bankAccountInsightsScoreLookup" -> "1216552f-df79-40a8-9941-45b2dca6be6c"
        )

        val ai = AuditItem.fromMap("foo.", ra)

        ai.data should contain theSameElementsAs Map(
          "foo.sortCode" -> "123456",
          "foo.accountNumber" -> "12345678",
          "foo.riskScore" -> "100",
          "foo.reason" -> "ACCOUNT_NOT_ON_WATCH_LIST",
          "foo.bankAccountInsightsScoreLookup" -> "1216552f-df79-40a8-9941-45b2dca6be6c"
        )
      }

      "then appVersion is included in the audit data" in {
        val audit = mock[AuditServiceSeam]
        val headerCarrier = HeaderCarrier(requestId = Some(RequestId("REQUEST_ID")))
        val client = new AuditServiceImpl(audit, "appName", "version1")
        implicit val request: Request[_] = mock[Request[_]]
        when(request.headers).thenReturn(Headers())

        client.succeeded(Map.empty, headerCarrier, None)

        val expected = Map("appVersion" -> "version1")
        verify(audit).sendDataEvent(
          Map(
            "clientIP" -> "-",
            "path" -> "",
            "X-Session-ID" -> "-",
            "Akamai-Reputation" -> "-",
            "X-Request-ID" -> "REQUEST_ID",
            "deviceID" -> "-",
            "clientPort" -> "-",
            "transactionName" -> ""), expected, "appName")
      }
    }
  }
}
