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

package uk.gov.hmrc.bankaccountinsightsproxy.controllers

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.http.HeaderNames
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.test.Helpers._
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.DownstreamConnector

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class InsightsControllerIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with MockitoSugar {
  private val defaultDuration = 5.seconds

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  private lazy val mockDownstreamConnector: DownstreamConnector = {
    val _mock = mock[DownstreamConnector]
    when(_mock.checkConnectivity(any(), any())(any())).thenReturn(Future.successful(true))
    when(_mock.forward(any(), any(), any())(any())).thenReturn(Future.successful(Ok("{}")))
    _mock
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[DownstreamConnector].toInstance(mockDownstreamConnector))
      .configure("metrics.enabled" -> false)
      .build()

  "InsightsController" should {
    "respond with OK status" when {
      "valid json payload is provided to the /check/insights endpoint" in {
        val response = Await.result(
          wsClient.url(s"${baseUrl}/bank-account-insights/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
            .post("""{"sortCode":"123456", "accountNumber":"12345678"}"""), defaultDuration)

        response.status shouldBe OK
      }

      "valid json payload is provided to the /ipp endpoint" in {
        val response = Await.result(
          wsClient.url(s"${baseUrl}/bank-account-insights/ipp")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
            .post("""{"sortCode":"123456", "accountNumber":"12345678"}"""), defaultDuration)

        response.status shouldBe OK
      }
    }

    "respond with BAD_REQUEST status" when {
      "invalid json payload is provided to the /check/insights endpoint" in {
        val response = Await.result(
          wsClient.url(s"${baseUrl}/bank-account-insights/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
            .post("""{"sortCode":"123456", "accountNumber":"12345678}"""), defaultDuration)

        response.status shouldBe BAD_REQUEST
        val responseBodyJs = response.body[JsValue]
        (responseBodyJs \ "statusCode").as[Int] shouldBe BAD_REQUEST
        (responseBodyJs \ "message").as[String] shouldBe "bad request, cause: invalid json"
      }

      "invalid json payload is provided to the /ipp endpoint" in {
        val response = Await.result(
          wsClient.url(s"${baseUrl}/bank-account-insights/ipp")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
            .post("""{"sortCode":"123456", "accountNumber":"12345678}"""), defaultDuration)

        response.status shouldBe BAD_REQUEST
        val responseBodyJs = response.body[JsValue]
        (responseBodyJs \ "statusCode").as[Int] shouldBe BAD_REQUEST
        (responseBodyJs \ "message").as[String] shouldBe "bad request, cause: invalid json"
      }
    }
  }
}
