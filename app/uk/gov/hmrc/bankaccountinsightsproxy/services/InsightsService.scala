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

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.RiskListsConnector
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.BankAccountInsightsResponse
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.risklist_response_codes.{ACCOUNT_NOT_ON_WATCH_LIST, ACCOUNT_ON_WATCH_LIST}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InsightsService @Inject()(
                                 insightsConnector: RiskListsConnector,
                                 correlationIdService: CorrelationIdService,
                                 metrics: Metrics) {

  val onWatchListCounter: Counter = metrics.defaultRegistry.counter("responses.account.onWatchList")
  val notOnWatchListCounter: Counter = metrics.defaultRegistry.counter("responses.account.notOnWatchList")

  def insights(bankAccountInsightsRequest: InsightsRequest)(implicit
                                                            ec: ExecutionContext,
                                                            hc: HeaderCarrier
  ): Future[Either[String, BankAccountInsightsResponse]] = {
    val bankAccountInsightsCorrelationId = correlationIdService.get
    insightsConnector.isOnRejectList(bankAccountInsightsRequest) map {
      case Right(true) =>
        onWatchListCounter.inc()
        Right(BankAccountInsightsResponse(bankAccountInsightsCorrelationId, 100, ACCOUNT_ON_WATCH_LIST))
      case Right(false) =>
        notOnWatchListCounter.inc()
        Right(BankAccountInsightsResponse(bankAccountInsightsCorrelationId, 0, ACCOUNT_NOT_ON_WATCH_LIST))
      case Left(em) => Left(em)
    }
  }
}
