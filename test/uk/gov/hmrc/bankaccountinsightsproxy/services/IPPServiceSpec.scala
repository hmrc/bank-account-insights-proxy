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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.IPPConnector
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.{IPPResponse, IPPResponseComponent, IPPResponseComponentValue}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class IPPServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite {
  private val defaultDuration = 5 seconds

  override implicit lazy val app: Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder().build()
  }

  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val ippFoundCounter: Counter = mock[Counter]
  val ippNotCounter: Counter = mock[Counter]

  val registry: MetricRegistry = mock[MetricRegistry]
  val connector: IPPConnector = mock[IPPConnector]
  val correlationIdService: CorrelationIdService = mock[CorrelationIdService]

  when(registry.counter(meq("responses.account.ippFound"))).thenReturn(ippFoundCounter)
  when(registry.counter(meq("responses.account.ippNotFound"))).thenReturn(ippNotCounter)

  import com.kenshoo.play.metrics.Metrics

  val metrics: Metrics = new Metrics {
    val defaultRegistry: MetricRegistry = registry
    override def toJson: String = ???
  }

  val service = new IPPService(connector, correlationIdService, metrics)

  "When information is found for an account" should {
    "return a response containing that information" in {
      reset(ippNotCounter, ippFoundCounter, connector)
      reset(connector)
      val ippResponse = IPPResponse("999999", "12345678", Some("correlation-id"), List(
        IPPResponseComponent("test-component-01", 1, List(
          IPPResponseComponentValue("value-a", 2, LocalDateTime.of(2023, 1, 13, 13, 23)),
          IPPResponseComponentValue("value-b", 5, LocalDateTime.of(2023, 1, 11, 15, 33))
        )),
        IPPResponseComponent("test-component-02", 3, List(
          IPPResponseComponentValue("value-a", 7, LocalDateTime.of(2023, 1, 13, 13, 23)),
          IPPResponseComponentValue("value-b", 1, LocalDateTime.of(2023, 1, 11, 15, 33))
        ))
      ))
      when(connector.ipp(any())(any(), any())).thenReturn(Future.successful(Right(ippResponse)))
      when(correlationIdService.get).thenReturn("correlation-id")

      val Right(response) = Await.result(service.ipp(InsightsRequest("999999", "12345678")), defaultDuration)

      response.correlationId shouldBe Some("correlation-id")
      response.ippComponents should not be empty
      response.ippComponents should have length 2
      response.ippComponents.head.componentValues should have length 2
      response.ippComponents.drop(1).head.componentValues should have length 2

      verify(ippFoundCounter, times(1)).inc()
      verify(ippNotCounter, never()).inc()
    }
  }

  "When information is not found for an account" should {
    "return a response containing no information" in {
      reset(ippNotCounter, ippFoundCounter, connector)
      reset(connector)
      val ippResponse = IPPResponse("999999", "12345678", Some("correlation-id"), List())
      when(connector.ipp(any())(any(), any())).thenReturn(Future.successful(Right(ippResponse)))
      when(correlationIdService.get).thenReturn("correlation-id")

      val Right(response) = Await.result(service.ipp(InsightsRequest("999999", "12345678")), defaultDuration)

      response.correlationId shouldBe Some("correlation-id")
      response.ippComponents shouldBe empty

      verify(ippFoundCounter, never()).inc()
      verify(ippNotCounter, times(1)).inc()
    }
  }
}
