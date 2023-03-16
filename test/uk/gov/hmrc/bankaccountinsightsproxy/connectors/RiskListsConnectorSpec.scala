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

package uk.gov.hmrc.bankaccountinsightsproxy.connectors

import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.routing.sird.{POST => SPOST, _}
import play.core.server.{Server, ServerConfig}
import uk.gov.hmrc.bankaccountinsightsproxy.config.AppConfig
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class RiskListsConnectorSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite {

  val testPort = 11222
  val testToken = "auth-token"

  override implicit lazy val app: Application = {
    SharedMetricRegistries.clear()

    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.bank-account-insights.port" -> testPort,
        "microservice.services.bank-account-insights.authToken" -> testToken)
      .build()
  }

  private val connector = app.injector.instanceOf[RiskListsConnector]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
  private val defaultDuration = 60 seconds

  "POST /reject/bank-account" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "Return true if the account is on the reject list" in {
      val auth = AppConfig.createAuth("bank-account-insights-proxy", testToken)

      val response =
        """{
          | "result": true
          |}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(testPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/reject/bank-account") =>
            Action(req => {
              r.headers.get(HeaderNames.AUTHORIZATION) shouldBe Some(s"Basic $auth")
              req.body.asJson shouldBe Some(Json.parse("""{"sortCode":"123456","accountNumber":"12345678"}"""))
              Ok(response).withHeaders("Content-Type" -> "application/json")
            })
        }
      } { _ =>
        val result = Await.result(connector.isOnRejectList(InsightsRequest("123456", "12345678")), defaultDuration)
        result shouldBe Right(true)
      }
    }

    "Handle a 500 error" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(testPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/reject/bank-account") =>
            Action(InternalServerError)
        }
      } { _ =>
        val result = Await.result(connector.isOnRejectList(InsightsRequest("123456", "12345678")), defaultDuration)
        result shouldBe a[Left[Throwable, Boolean]]
      }
    }
  }
}
