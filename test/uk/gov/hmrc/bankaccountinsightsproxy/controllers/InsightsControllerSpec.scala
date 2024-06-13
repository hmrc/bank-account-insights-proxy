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

package uk.gov.hmrc.bankaccountinsightsproxy
package controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.*
import play.api.mvc.{ControllerComponents, Result}
import play.api.routing.sird.POST as SPOST
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.core.server.{Server, ServerConfig}
import uk.gov.hmrc.bankaccountinsightsproxy.config.AppConfig

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class InsightsControllerSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite:
  val insightsPort = 11222
  val authToken = "test-token"

  implicit val controllerComponents: ControllerComponents =
    Helpers.stubControllerComponents()

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.bank-account-insights.port" -> insightsPort
    )
    .configure(
      "microservice.services.bank-account-insights.authToken" -> authToken
    )
    .configure("microservice.services.access-control.enabled" -> true)
    .configure(
      "microservice.services.access-control.allow-list.0" -> "example-service"
    )
    .configure(
      "microservice.services.access-control.allow-list.1" -> "proxy-service"
    )
    .overrides(bind[ControllerComponents].toInstance(controllerComponents))
    .build()

  private val controller = app.injector.instanceOf[InsightsController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "POST /check/insights" when {
    val path = "/check/insights"
    val request =
      """{"account": {"accountNumber": "12345667", "sortCode": "123456"}}""".stripMargin
    val response =
      """{
        |  "score": "100",
        |  "reason": "ACCOUNT_ON_WATCH_LIST",
        |  "correlationId": "12345-12345-12345-12345"
        |}""".stripMargin

    "the user-agent is on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "example-service",
        "Content-Type" -> "application/json"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "there are multiple user-agents all of which are present on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "proxy-service",
        "User-Agent" -> "example-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "there is a comma separated user-agent and all components of it are present on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "proxy-service,example-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "the user-agent is NOT on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "another-service",
          "User-Agent" -> "another-service",
          "Content-Type" -> "application/json"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there are multiple user-agents but none are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "another-service",
          "User-Agent" -> "more-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is a comma separated user-agent but only one of it's components are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "proxy-service,another-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is a comma separated user-agent but none of it's component are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "another-service,more-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is an originator id in the request which matches a value on the allow list, but the user-agent does not" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "OriginatorId" -> "example-service",
        "User-Agent" -> "invalid-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }
  }

  "POST /ipp" when {
    val path = "/ipp"
    val request =
      """{"account": {"accountNumber": "12345667", "sortCode": "123456"}}""".stripMargin
    val response =
      """{
        |  "score": "100",
        |  "reason": "ACCOUNT_ON_WATCH_LIST",
        |  "correlationId": "12345-12345-12345-12345"
        |}""".stripMargin

    "the user-agent is on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "example-service",
        "Content-Type" -> "application/json"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "there are multiple user-agents all of which are present on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "proxy-service",
        "User-Agent" -> "example-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "there is a comma separated user-agent and all components of it are present on the allow list" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "User-Agent" -> "proxy-service,example-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }

    "the user-agent is NOT on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "another-service",
          "User-Agent" -> "another-service",
          "Content-Type" -> "application/json"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there are multiple user-agents but none are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "another-service",
          "User-Agent" -> "more-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is a comma separated user-agent but only one of it's components are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "proxy-service,another-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is a comma separated user-agent but none of it's component are on the allow list" should {
      "return forbidden" in {
        val headers = Seq(
          "True-Calling-Client" -> "example-service",
          "User-Agent" -> "another-service,more-service",
          "Content-Type" -> "application/json",
          "Authorization" -> "1234"
        )
        val result = controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )

        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "there is an originator id in the request which matches a value on the allow list, but the user-agent does not" should {
      val headers = Seq(
        "True-Calling-Client" -> "example-service",
        "OriginatorId" -> "example-service",
        "User-Agent" -> "invalid-service",
        "Content-Type" -> "application/json",
        "Authorization" -> "1234"
      )

      behave like downstreamConnectorEndpoint(path, response) { () =>
        controller.checkInsights()(
          FakeRequest("POST", path)
            .withJsonBody(Json.parse(request))
            .withHeaders(headers: _*)
        )
      }
    }
  }

  def downstreamConnectorEndpoint(url: String, response: String)(
      invoke: () => Future[Result]
  ): Unit = {
    "forward a 200 response from the downstream service" in:

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) {
        components =>
          import components.{defaultActionBuilder => Action}
          {
            case r @ SPOST(u) if u.path == url =>
              r.headers.get("True-Calling-Client") shouldBe Some(
                "example-service"
              )
              r.headers.get("Authorization") shouldBe Some(
                "Basic " + AppConfig.createAuth(
                  "bank-account-insights-proxy",
                  authToken
                )
              )
              Action(
                Ok(response).withHeaders("Content-Type" -> "application/json")
              )
          }
      } { _ =>

        val result = invoke()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }

    "forward a 400 response from the downstream service" in:
      val errorResponse =
        """{"code": "MALFORMED_JSON", "path.missing: Subject"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) {
        components =>
          import components.{defaultActionBuilder => Action}
          {
            case r @ SPOST(u) if u.path == url =>
              Action(
                BadRequest(errorResponse).withHeaders(
                  "Content-Type" -> "application/json"
                )
              )
          }
      } { _ =>
        val result = invoke()
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }

    "handle a malformed json payload" in:
      val errorResponse =
        """{"code": "MALFORMED_JSON", "path.missing: Subject"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) {
        components =>
          import components.{defaultActionBuilder => Action}
          {
            case r @ SPOST(u) if u.path == url =>
              Action(
                BadRequest(errorResponse).withHeaders(
                  "Content-Type" -> "application/json"
                )
              )
          }
      } { _ =>
        val result = invoke()
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }

    "return bad gateway if there is no connectivity to the downstream service" in:
      val errorResponse =
        """{"code": "REQUEST_DOWNSTREAM", "desc": "An issue occurred when the downstream service tried to handle the request"}""".stripMargin

      val result = invoke()
      status(result)(30 seconds) shouldBe Status.BAD_GATEWAY
      contentAsString(result) shouldBe errorResponse
  }
