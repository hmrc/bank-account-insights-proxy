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
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.IPPConnector
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.IPPResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IPPService @Inject()(
    ippConnector: IPPConnector,
    correlationIdService: CorrelationIdService,
    metrics: Metrics) {

  val ippFound: Counter = metrics.defaultRegistry.counter("responses.account.ippFound")
  val ippNotFound: Counter = metrics.defaultRegistry.counter("responses.account.ippNotFound")

  def ipp(bankAccountInsightsRequest: InsightsRequest)(implicit
                                                       ec: ExecutionContext,
                                                       hc: HeaderCarrier
  ): Future[Either[String, IPPResponse]] = {
    val bankAccountInsightsCorrelationId = correlationIdService.get
    ippConnector.ipp(bankAccountInsightsRequest) map {
      case Right(ippResponse) =>
        if(ippResponse.ippComponents.nonEmpty) ippFound.inc()
        else ippNotFound.inc()
        Right(ippResponse.copy(correlationId = Some(bankAccountInsightsCorrelationId)))
      case l@Left(e) => ippNotFound.inc()
        l
    }
  }

}
