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

package uk.gov.hmrc.bankaccountinsightsproxy.services

import play.api.mvc.Request
import uk.gov.hmrc.bankaccountinsightsproxy.model.audit.AuditItem
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

import scala.concurrent.ExecutionContext

trait AuditService {

  def succeeded(tags: Map[String, String], headerCarrier: HeaderCarrier, userAgent: Option[String], items: AuditItem*)(implicit request: Request[_], ec: ExecutionContext): Unit
}

class AuditServiceImpl(audit: AuditServiceSeam, appName: String, appVersion: String) extends AuditService {

  def succeeded(tags: Map[String, String], headerCarrier: HeaderCarrier, userAgent: Option[String], items: AuditItem*)(implicit request: Request[_], ec: ExecutionContext): Unit = {

    val detail = items.toSeq.flatMap(_.data).toMap ++ userAgent.map(ua => "userAgent" -> ua)
    val augmented = detail + ("appVersion" -> appVersion)
    val mergedTags = tags ++ AuditService.apiPlatformHeaders() ++ AuditExtensions.auditHeaderCarrier(headerCarrier).toAuditTags("", "")
    audit.sendDataEvent(mergedTags, augmented, appName)
  }
}

object AuditService {
  object apiPlatform {
    val xClientAuthorisationToken = "X-Client-Authorization-Token"
    val xClientId = "X-Client-ID"
    val xApplicationId = "X-Session-ID"
  }

  def apiPlatformHeaders()(implicit request: Request[_]): Map[String, String] = {
    import apiPlatform._
    val headers = request.headers
    List(
      headers.get(xClientAuthorisationToken).map(v => xClientAuthorisationToken -> v),
      headers.get(xClientId).map(v => xClientId -> v),
      headers.get(xApplicationId).map(v => xApplicationId -> v)
    ).flatten.toMap.view.mapValues(vs => vs.mkString).toMap
  }
}

class AuditServiceSeam(audit: Audit) {

  def sendDataEvent(tags: Map[String, String], detail: Map[String, String], appName: String, auditType: AuditType = AuditType.Default)(implicit ec: ExecutionContext): Unit = {
    val event = DataEvent(auditSource = appName, auditType = auditType.name, detail = detail, tags = tags)
    audit.sendDataEvent(event)
  }
}

sealed abstract class AuditType(val name: String)

object AuditType {
  case object Default extends AuditType("BankAccountInsightsLookup")
}
