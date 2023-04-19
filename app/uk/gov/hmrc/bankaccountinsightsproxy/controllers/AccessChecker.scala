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
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.mvc.Request
import uk.gov.hmrc.bankaccountinsightsproxy.config.ExtendedServicesConfig
import uk.gov.hmrc.bankaccountinsightsproxy.controllers.AccessChecker.{accessControlAllowListAbsoluteKey, accessControlAllowListKey, accessControlEnabledKey, accessRequestFormUrlKey}

import javax.inject.{Inject, Singleton}

@Singleton
class AccessChecker @Inject()(config: Configuration) extends ExtendedServicesConfig(config) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val accessRequestFormUrl: String = getRequiredConfString(accessRequestFormUrlKey)

  private val checkAllowList: Boolean = getRequiredConfString(accessControlEnabledKey).toBoolean
  private val allowedClients: Set[String] = getStringSeq(accessControlAllowListKey,
    if (checkAllowList) throw new RuntimeException(s"Could not find config $accessControlAllowListAbsoluteKey") else Seq()).toSet

  def isClientAllowed(client: Option[String]): Boolean =
    !checkAllowList || client.fold(false)(allowedClients.contains)

  def forbiddenResponse(client: Option[String]): String =
    s"""{
       |"code": 403,
       |"description": "'${client.getOrElse("Unknown Client")}' is not authorized to use this service. Please complete '${accessRequestFormUrl}' to request access."
       |}""".stripMargin

  def getClientFromUserAgent[T](req: Request[T]): Option[String] = {
    req.headers.get("OriginatorId") match {
      case Some(oId) => logger.warn(s"An OriginatorId was provided: $oId")
      case _ =>
    }

    req.headers.get(HeaderNames.USER_AGENT)
      .flatMap(userAgent => userAgent.split(",").find(ua => ua != "bank-account-gateway"))
  }
}

object AccessChecker {
  val accessRequestFormUrlKey = "access-control.request.formUrl"
  val accessRequestFormUrlAbsoluteKey = s"microservice.services.$accessRequestFormUrlKey"
  val accessControlEnabledKey = "access-control.enabled"
  val accessControlEnabledAbsoluteKey = s"microservice.services.$accessControlEnabledKey"
  val accessControlAllowListKey = "access-control.allow-list"
  val accessControlAllowListAbsoluteKey = s"microservice.services.$accessControlAllowListKey"
}
