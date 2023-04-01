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

import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.bankaccountinsightsproxy.config.AppConfig
import uk.gov.hmrc.bankaccountinsightsproxy.connectors.DownstreamConnector
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InsightsController @Inject()(connector: DownstreamConnector,
                                   config: AppConfig,
                                   internalAuth: BackendAuthComponents,
                                   cc: ControllerComponents
                                  )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def checkInsights(): Action[AnyContent] = forwardIfAuthorised(ResourceLocation("check"))

  def ipp(): Action[AnyContent] = forwardIfAuthorised(ResourceLocation("ipp"))

  private def forwardIfAuthorised(resourceLocation: ResourceLocation) = {
    val permission = Predicate.Permission(
      Resource(ResourceType("bank-account-insights"), resourceLocation),
      IAAction("READ"))

    internalAuth.authorizedAction(permission).async(parse.anyContent) {
      implicit request: Request[AnyContent] =>

        val path = request.target.uri.toString
        val url = s"${config.bankAccountInsightsBaseUrl}$path"

        connector.forward(request, url, config.bankAccountInsightsAuthToken)
    }
  }
}
