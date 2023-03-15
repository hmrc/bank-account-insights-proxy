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

import org.slf4j.LoggerFactory
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.bankaccountinsightsproxy.model.audit.AuditItem
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest.implicits._
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.BankAccountInsightsResponse.implicits._
import uk.gov.hmrc.bankaccountinsightsproxy.services.{AuditService, InsightsService}
import uk.gov.hmrc.bankaccountinsightsproxy.utils.json.simplifyJsonErrors
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InsightsController @Inject()(
                                    insightsService: InsightsService,
                                    audit: AuditService,
                                    internalAuth: BackendAuthComponents,
                                    cc: ControllerComponents
                                             )(implicit ec: ExecutionContext)
  extends BackendController(cc) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val permission: Predicate.Permission = Predicate.Permission(Resource(
    ResourceType("bank-account-insights"),
    ResourceLocation("check")),
    IAAction("READ"))

  def insights(): Action[JsValue] = internalAuth.authorizedAction(permission).async(parse.json) {
    implicit request: Request[JsValue] => {
      withValidRiskRequest { bankAccountDetailsRiskRequest =>
        insightsService
          .insights(bankAccountDetailsRiskRequest)
          .map {
            case Right(riskResponse) => audit.succeeded(
              tags = Map(),
              headerCarrier = hc(request),
              userAgent = request.headers.get(HeaderNames.USER_AGENT),
              items = AuditItem.fromBankAccountInsightsResponse(bankAccountDetailsRiskRequest, riskResponse)
            )
              Ok(Json.toJson(riskResponse))
            case Left(msg) =>
              logger.error(s"Error occurred getting risk list response: ${msg}")
              InternalServerError(s"""{"code":"ERROR", "message": "${msg}"}""")
          }
      }
    }
  }

  private def withValidRiskRequest(f: InsightsRequest => Future[Result])(implicit request: Request[JsValue]): Future[Result] =
    request.body.validate[InsightsRequest] match {
      case JsSuccess(req, _) =>
        f(req)
      case JsError(payloadErrors) =>
        Future.successful(BadRequest(simplifyJsonErrors(payloadErrors)))
    }
}
