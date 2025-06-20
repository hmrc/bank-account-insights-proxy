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

import play.api.http.HeaderNames
import play.api.mvc.Request
import play.api.{Configuration, Logging}
import uk.gov.hmrc.bankaccountinsightsproxy.config.ExtendedServicesConfig
import uk.gov.hmrc.bankaccountinsightsproxy.controllers.AccessChecker.{accessControlAllowListAbsoluteKey, accessControlAllowListKey, accessControlEnabledKey, accessRequestFormUrlKey}

import javax.inject.{Inject, Singleton}

@Singleton
class AccessChecker @Inject()(config: Configuration) extends ExtendedServicesConfig(config) with Logging {

  private val accessRequestFormUrl: String = getRequiredConfString(accessRequestFormUrlKey)

  private val checkAllowList: Boolean = getRequiredConfString(accessControlEnabledKey).toBoolean
  private val allowedClients: Set[String] = getStringSeq(accessControlAllowListKey,
    if (checkAllowList) throw new RuntimeException(s"Could not find config $accessControlAllowListAbsoluteKey") else Seq()).toSet

  def areClientsAllowed(clients: Seq[String]): Boolean =
    !checkAllowList || clients.forall(allowedClients.contains)

  def forbiddenResponse(clients: Seq[String]): String =
    s"""{
       |"code": 403,
       |"description": "One or more user agents in '${clients.mkString(",")}' are not authorized to use this service. Please complete '${accessRequestFormUrl}' to request access."
       |}""".stripMargin

  def getClientsFromRequest[T](req: Request[T]): Seq[String] = {
    val originator = req.headers.get("OriginatorId")

    if (originator.isDefined)
      Seq(originator.get)
    else
      req.headers.getAll(HeaderNames.USER_AGENT).flatMap(_.split(","))
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
