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

import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest.implicits._
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.BankAccountInsightsResponse
import uk.gov.hmrc.bankaccountinsightsproxy.services.{AuditService, InsightsService}
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InsightsControllerSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite {

  override implicit lazy val app: Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder().build()
  }

  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  private val mockInsightsService = mock[InsightsService]
  private val mockStubBehaviour = mock[StubBehaviour]
  private val mockAudit = mock[AuditService]

  when(mockInsightsService.insights(any())(any(), any()))
    .thenReturn(
      Future.successful(Right(
        BankAccountInsightsResponse(
          "c33b596c-2cdd-4bf7-a20c-8efd1b32802f",
          10,
          "Looks fine"
        ))
      )
    )


  val expectedPermission: Predicate.Permission = Predicate.Permission(Resource(
    ResourceType("bank-account-insights"),
    ResourceLocation("check")),
    IAAction("READ"))

  when(mockStubBehaviour.stubAuth(Some(expectedPermission), EmptyRetrieval)).thenReturn(Future.successful())

  implicit val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()

  private val controller = new InsightsController(
    mockInsightsService,
    mockAudit,
    BackendAuthComponentsStub(mockStubBehaviour),
    controllerComponents
  )

  "InsightsController" should {
    "return 200" when {
      "POST /cip-insights/bank-account with valid request" in {
        val payload =
          InsightsRequest("123456", "12345678")
        val fakeRequest =
          FakeRequest("POST", "/cip-insights/bank-account")
            .withHeaders("content-type" -> "application/json", "Authorization" -> "1234")
            .withBody(Json.toJson(payload))

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.OK
      }
    }

    "return 400" when {
      "POST /cip-insights/bank-account with invalid request" in {
        val payload =
          InsightsRequest("12345", "12345678")
        val fakeRequest =
          FakeRequest("POST", "/cip-insights/bank-account")
            .withHeaders("content-type" -> "application/json", "Authorization" -> "1234")
            .withBody(Json.toJson(payload))

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }
}
