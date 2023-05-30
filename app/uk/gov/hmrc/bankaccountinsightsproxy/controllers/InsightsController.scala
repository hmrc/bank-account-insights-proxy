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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.bankaccountinsightsproxy.config.AppConfig
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.DownstreamConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InsightsController @Inject()(connector: DownstreamConnector,
                                   config: AppConfig,
                                   clientAllowListChecker: AccessChecker,
                                   cc: ControllerComponents
                                  )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private val logger = Logger(this.getClass.getSimpleName)

  private def url(path: String) = s"${config.bankAccountInsightsBaseUrl}$path"

  def checkInsights: Action[AnyContent] = forwardIfAllowed()

  def ipp: Action[AnyContent] = forwardIfAllowed()

  private def forwardIfAllowed() = Action.async(parse.anyContent) {
    implicit request: Request[AnyContent] =>

      val callingClients = clientAllowListChecker.getClientsFromRequest(request)
      if (!clientAllowListChecker.areClientsAllowed(callingClients)) Future.successful {
        Forbidden(Json.parse(clientAllowListChecker.forbiddenResponse(callingClients)))
      } else {
        connector.forward(request, url(request.target.uri.toString), config.bankAccountInsightsAuthToken)
      }
  }

  connector.checkConnectivity(url("/check/insights"), config.bankAccountInsightsAuthToken).map {
    case true => logger.warn("Downstream connectivity to insights service successfully established")
    case _ => logger.error("Downstream connectivity check to insights service FAILED")
  }
}
