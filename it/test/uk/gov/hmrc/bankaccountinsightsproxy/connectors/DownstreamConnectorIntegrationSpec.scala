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

package uk.gov.hmrc.bankaccountinsightsproxy.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import org.apache.pekko.stream.Materializer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.bankaccountinsightsproxy.support.{IntegrationBaseSpec, WireMockHelper}

import scala.concurrent.Future

class DownstreamConnectorIntegrationSpec extends IntegrationBaseSpec with ScalaFutures with IntegrationPatience {
  implicit lazy val materializer: Materializer = app.materializer

  override def serviceConfig: Map[String, Any] = Map()

  class Test {
    lazy val connector: DownstreamConnector = app.injector.instanceOf[DownstreamConnector]
  }

  "DownstreamConnector" should {

    "return a successful response when the downstream service returns a 200" in new Test {
      stubFor(
        post(urlEqualTo("/test-endpoint"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{"message": "success"}""")
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
          )
      )

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/test-endpoint")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withJsonBody(Json.parse("""{"key": "value"}"""))

      val result: Future[Result] = connector.forward(request, s"http://localhost:${WireMockHelper.wireMockPort}/test-endpoint", "authToken")

      play.api.test.Helpers.status(result) shouldBe 200
      contentType(result) shouldBe Some("application/json")
      contentAsString(result) should include("success")
    }

    "return a BadGateway response when the downstream service returns a 502" in new Test {
      stubFor(
        post(urlEqualTo("/test-endpoint"))
          .willReturn(
            aResponse()
              .withStatus(502)
              .withBody("""{"code": "BAD_GATEWAY", "desc": "Downstream service is unavailable"}""")
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
          )
      )

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/test-endpoint")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withJsonBody(Json.parse("""{"key": "value"}"""))

      val result: Future[Result] = connector.forward(request, s"http://localhost:${WireMockHelper.wireMockPort}/test-endpoint", "authToken")

      play.api.test.Helpers.status(result) shouldBe 502
      contentType(result) shouldBe Some("application/json")
      contentAsString(result) should include("BAD_GATEWAY")
    }

    "return a MethodNotAllowed response when the request method is not supported" in new Test {
      val request: FakeRequest[AnyContent] = FakeRequest(GET, "/test-endpoint")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result: Future[Result] = connector.forward(request, s"http://localhost:${WireMockHelper.wireMockPort}/test-endpoint", "authToken")

      play.api.test.Helpers.status(result) shouldBe 405
      contentAsString(result) should include("UNSUPPORTED_METHOD")
    }

    "return an InternalServerError response when there is an issue forwarding the request" in new Test {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/test-endpoint")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withJsonBody(Json.parse("""{"key": "value"}"""))

      // Simulate an error in the connector
      val result: Future[Result] = connector.forward(request, "invalid-url", "authToken")

      play.api.test.Helpers.status(result) shouldBe 500
      contentType(result) shouldBe Some("application/json")
      contentAsString(result) should include("REQUEST_FORWARDING")
    }
  }
}