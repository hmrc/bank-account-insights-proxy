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

import akka.stream.Materializer
import com.codahale.metrics.{Counter, MetricRegistry, SharedMetricRegistries}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.RiskListsConnector
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.risklist_response_codes.{ACCOUNT_NOT_ON_WATCH_LIST, ACCOUNT_ON_WATCH_LIST}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class InsightsServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  private val defaultDuration = 5 seconds

  override implicit lazy val app: Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder().build()
  }

  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val onWatchListCounter: Counter = mock[Counter]
  val notOnWatchListCounter: Counter = mock[Counter]

  val registry: MetricRegistry = mock[MetricRegistry]
  val connector: RiskListsConnector = mock[RiskListsConnector]
  val correlationIdService: CorrelationIdService = mock[CorrelationIdService]

  when(registry.counter(meq("responses.account.onWatchList"))).thenReturn(onWatchListCounter)
  when(registry.counter(meq("responses.account.notOnWatchList"))).thenReturn(notOnWatchListCounter)

  import com.kenshoo.play.metrics.Metrics

  val metrics: Metrics = new Metrics {
    val defaultRegistry: MetricRegistry = registry
    override def toJson: String = ???
  }

  val service = new InsightsService(connector, correlationIdService, metrics)

  "When an account is on the reject list" should {
    "return a score of 100 and a reason code ACCOUNT_ON_WATCH_LIST" in {
      reset(notOnWatchListCounter, onWatchListCounter, connector)
      reset(connector)
      when(connector.isOnRejectList(any())(any(), any())).thenReturn(Future.successful(Right(true)))
      when(correlationIdService.get).thenReturn("correlation-id")

      val result = Await.result(service.insights(InsightsRequest("999999", "12345678")), defaultDuration)
      val response = result.right.get

      response.riskScore shouldBe 100
      response.reason shouldBe ACCOUNT_ON_WATCH_LIST

      verify(onWatchListCounter, times(1)).inc()
      verify(notOnWatchListCounter, never()).inc()
    }
  }

  "When an account is not on the reject list" should {
    "return a score of 0 and a reason code ACCOUNT_NOT_ON_WATCH_LIST" in {
      reset(notOnWatchListCounter, onWatchListCounter, connector)
      when(connector.isOnRejectList(any())(any(), any())).thenReturn(Future.successful(Right(false)))
      when(correlationIdService.get).thenReturn("correlation-id")

      val result = Await.result(service.insights(InsightsRequest("999999", "12345678")), defaultDuration)
      val response = result.right.get

      response.riskScore shouldBe 0
      response.reason shouldBe ACCOUNT_NOT_ON_WATCH_LIST

      verify(notOnWatchListCounter, times(1)).inc()
      verify(onWatchListCounter, never()).inc()
    }
  }

  "When an error occurs calling attribute risk lists" should {
    "return the error message" in {
      reset(notOnWatchListCounter, onWatchListCounter, connector)
      when(connector.isOnRejectList(any())(any(), any())).thenReturn(Future.successful(Left("Out of cheese")))
      when(correlationIdService.get).thenReturn("correlation-id")

      val result = Await.result(service.insights(InsightsRequest("999999", "12345678")), defaultDuration)
      result shouldBe Left("Out of cheese")
    }
  }
}
