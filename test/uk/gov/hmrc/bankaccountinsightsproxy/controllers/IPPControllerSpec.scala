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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.bankaccountinsightsproxy.controllers
//
//import org.apache.pekko.stream.Materializer
//import com.codahale.metrics.SharedMetricRegistries
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.mockito.MockitoSugar.mock
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import play.api.Application
//import play.api.http.Status
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.json.Json
//import play.api.mvc.ControllerComponents
//import play.api.mvc.Results._
//import play.api.routing.sird.{POST => SPOST, _}
//import play.api.test.Helpers._
//import play.api.test.{FakeRequest, Helpers}
//import play.core.server.{Server, ServerConfig}
//import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
//import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest.implicits._
//import uk.gov.hmrc.bankaccountinsightsproxy.model.response.{IPPResponse, IPPResponseComponent, IPPResponseComponentValue}
//import uk.gov.hmrc.bankaccountinsightsproxy.model.response.IPPResponse.Implicits.ippResponseFormat
//import uk.gov.hmrc.bankaccountinsightsproxy.services.{AuditService, IPPService}
//import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
//import uk.gov.hmrc.internalauth.client._
//import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
//
//import java.time.LocalDateTime
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class IPPControllerSpec
//  extends AnyWordSpec
//    with Matchers
//    with GuiceOneAppPerSuite {
//
//  val badPort = 9907
//  val expectedPermission: Predicate.Permission = Predicate.Permission(Resource(
//    ResourceType("bank-account-insights"),
//    ResourceLocation("check")),
//    IAAction("READ"))
//
//  private val mockStubBehaviour = mock[StubBehaviour]
//  when(mockStubBehaviour.stubAuth(Some(expectedPermission), EmptyRetrieval)).thenReturn(Future.successful())
//
//  implicit val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
//  val mockAudit: AuditService = mock[AuditService]
//
//
//  override implicit lazy val app: Application = {
//    SharedMetricRegistries.clear()
//    new GuiceApplicationBuilder()
//      .configure(
//        "microservice.services.bank-account-insights.port" -> badPort
//      )
//      .overrides(bind[BackendAuthComponents].toInstance(BackendAuthComponentsStub(mockStubBehaviour)))
//      .overrides(bind[AuditService].toInstance(mockAudit))
//      .build()
//  }
//
//  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
//
//  private val mockIPPService = mock[IPPService]
//
//  when(mockIPPService.ipp(any())(any(), any()))
//    .thenReturn(
//      Future.successful(
//        Right(IPPResponse("999999", "12345678", Some("correlation-id"), List(IPPResponseComponent("identifier", 1, List(
//          IPPResponseComponentValue("value-1", 1, LocalDateTime.of(2023, 1, 13, 13, 23)),
//          IPPResponseComponentValue("value-2", 3, LocalDateTime.of(2023, 2, 3, 17, 43))
//        )))))
//      )
//    )
//
//
//  private val controller = new IPPController(
//    mockIPPService,
//    mockAudit,
//    BackendAuthComponentsStub(mockStubBehaviour),
//    controllerComponents
//  )
//
//  private val ippController = app.injector.instanceOf[IPPController]
//
//  "IPPController" should {
//    "tinker" in {
//      Server.withRouterFromComponents(ServerConfig(port = Some(badPort), address = "localhost")) { components =>
//        import components.{defaultActionBuilder => Action}
//        {
//          case r @ SPOST(p"/ipp") =>
//            Action(Ok(
//              """{
//                |  "sortCode": "123457",
//                |  "accountNumber": "12345679",
//                |  "ippComponents": [
//                |    {
//                |      "identifier": "sa_utr",
//                |      "count": 1,
//                |      "componentValues": [
//                |        {
//                |          "value": "AB1324578724",
//                |          "numOfOccurrences": 3,
//                |          "lastSeen": "2023-01-10T12:35:00"
//                |        }
//                |      ]
//                |    },
//                |    {
//                |      "identifier": "vrn",
//                |      "count": 2,
//                |      "componentValues": [
//                |        {
//                |          "value": "2436679346",
//                |          "numOfOccurrences": 6,
//                |          "lastSeen": "2023-01-05T01:35:00"
//                |        },
//                |        {
//                |          "value": "8787948575",
//                |          "numOfOccurrences": 2,
//                |          "lastSeen": "2022-10-05T12:35:00"
//                |        }
//                |      ]
//                |    }
//                |  ]
//                |}""".stripMargin).withHeaders("Content-Type" -> "application/json"))
//        }
//      } { _ =>
//        val payload =
//          InsightsRequest("123456", "12345679")
//        val fakeRequest =
//          FakeRequest("POST", "/ipp")
//            .withHeaders(
//              "content-type" -> "application/json",
//              "True-Calling-Client" -> "example-service",
//              "Authorization" -> "1234")
//            .withBody(Json.toJson(payload))
//        val res = ippController.ipp()(fakeRequest)
//        val ippResponse = contentAsJson(res).as[IPPResponse]
//        val ippComponents = ippResponse.ippComponents
//        ippComponents should have size (2)
//        ippComponents.find(_.identifier == "sa_utr") shouldBe defined
//        val saUtrComponent = ippComponents.find(_.identifier == "sa_utr").get
//        saUtrComponent.count shouldBe 1
//        saUtrComponent.componentValues should have length 1
//        saUtrComponent.componentValues.head shouldBe IPPResponseComponentValue("AB1324578724", 3, LocalDateTime.of(2023, 1, 10, 12, 35))
//        ippComponents.find(_.identifier == "vrn") shouldBe defined
//        val vrnComponent = ippComponents.find(_.identifier == "vrn").get
//        vrnComponent.count shouldBe 2
//        vrnComponent.componentValues(0) shouldBe IPPResponseComponentValue("2436679346", 6, LocalDateTime.of(2023, 1, 5, 1, 35))
//        vrnComponent.componentValues(1) shouldBe IPPResponseComponentValue("8787948575", 2, LocalDateTime.of(2022, 10, 5, 12, 35))
//      }
//    }
//
//    "return 200" when {
//      "POST /ipp with valid request" in {
//        val payload =
//          InsightsRequest("123456", "12345678")
//        val fakeRequest =
//          FakeRequest("POST", "/ipp")
//            .withHeaders("content-type" -> "application/json", "Authorization" -> "1234")
//            .withBody(Json.toJson(payload))
//
//        val result = controller.ipp()(fakeRequest)
//        status(result) shouldBe Status.OK
//      }
//    }
//
//    "return 400" when {
//      "POST /ipp with invalid request" in {
//        val payload =
//          InsightsRequest("12345", "12345678")
//        val fakeRequest =
//          FakeRequest("POST", "/ipp")
//            .withHeaders("content-type" -> "application/json", "Authorization" -> "1234")
//            .withBody(Json.toJson(payload))
//
//        val result = controller.ipp()(fakeRequest)
//        status(result) shouldBe Status.BAD_REQUEST
//      }
//    }
//  }
//}
