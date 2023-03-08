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
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.{IPPResponse, IPPResponseComponent, IPPResponseComponentValue}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class IPPConnectorSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite {

  val testPort = 9907
  val testToken = "auth-token"

  override implicit lazy val app: Application = {
    SharedMetricRegistries.clear()

    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.bank-account-insights.port" -> testPort,
        "microservice.services.bank-account-insights.authToken" -> testToken)
      .build()
  }

  private val connector = app.injector.instanceOf[IPPConnector]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
  private val defaultDuration = 60 seconds

  "POST /ipp" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "Return true if the account is on the reject list" in {
      val auth = AppConfig.createAuth("bank-account-insights-proxy", testToken)

      val response =
        """{
          |  "sortCode": "123457",
          |  "accountNumber": "12345679",
          |  "ippComponents": [
          |    {
          |      "identifier": "sa_utr",
          |      "count": 1,
          |      "componentValues": [
          |        {
          |          "value": "AB1324578724",
          |          "numOfOccurrences": 3,
          |          "lastSeen": "2023-01-10T12:35:00"
          |        }
          |      ]
          |    },
          |    {
          |      "identifier": "vrn",
          |      "count": 2,
          |      "componentValues": [
          |        {
          |          "value": "2436679346",
          |          "numOfOccurrences": 6,
          |          "lastSeen": "2023-01-05T01:35:00"
          |        },
          |        {
          |          "value": "8787948575",
          |          "numOfOccurrences": 2,
          |          "lastSeen": "2022-10-05T12:35:00"
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin
      Server.withRouterFromComponents(ServerConfig(port = Some(testPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/ipp") =>
            Action(req => {
              r.headers.get(HeaderNames.AUTHORIZATION) shouldBe Some(s"Basic $auth")
              req.body.asJson shouldBe Some(Json.parse("""{"sortCode":"123456","accountNumber":"12345678"}"""))
              Ok(response).withHeaders("Content-Type" -> "application/json")
            })
        }
      } { _ =>
        val ippResponse = Await.result(connector.ipp(InsightsRequest("123456", "12345678")), defaultDuration)
        ippResponse shouldBe Right(IPPResponse("123457", "12345679", None, List(
          IPPResponseComponent("sa_utr", 1, List(IPPResponseComponentValue("AB1324578724", 3, LocalDateTime.of(2023, 1, 10, 12, 35)))),
          IPPResponseComponent("vrn", 2, List(
            IPPResponseComponentValue("2436679346", 6, LocalDateTime.of(2023, 1, 5, 1, 35)),
            IPPResponseComponentValue("8787948575", 2, LocalDateTime.of(2022, 10, 5, 12, 35))))
        )))
      }
    }

    "Handle a 500 error" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(testPort))) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r@SPOST(p"/ipp") =>
            Action(InternalServerError)
        }
      } { _ =>
        val result = Await.result(connector.ipp(InsightsRequest("123456", "12345678")), defaultDuration)
        result shouldBe Left("POST of 'http://localhost:9907/ipp' returned 500. Response body: ''")
      }
    }
  }
}
