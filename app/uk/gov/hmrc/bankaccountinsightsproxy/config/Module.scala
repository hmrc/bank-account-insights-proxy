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

package uk.gov.hmrc.bankaccountinsightsproxy.config

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import play.api.{Configuration, Environment}
import play.api.libs.ws
import play.api.libs.ws.{WSClient, WSProxyServer}
import uk.gov.hmrc.bankaccountinsightsproxy.services.{AuditService, AuditServiceImpl, AuditServiceSeam}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws.{WSProxy, WSProxyConfiguration}
import buildinfo.BuildInfo

import javax.inject.{Named, Singleton}
import scala.concurrent.duration._
import scala.language.postfixOps

class Module(environment: Environment, playConfig: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
  }

  @Provides
  @Named("external-http-client")
  def provideExternalHttpClient(
                                 auditConnector: HttpAuditing,
                                 wsClient: WSClient,
                                 actorSystem: ActorSystem
                               ): HttpClient = {
    new DefaultHttpClient(playConfig, auditConnector, wsClient, actorSystem)
      with WSProxy {
      override def wsProxyServer: Option[WSProxyServer] = {
        WSProxyConfiguration(s"proxy", playConfig)
      }

      override def buildRequest(
                                 url: String,
                                 headers: Seq[(String, String)] = Seq.empty
                               ): ws.WSRequest =
        super
          .buildRequest(url, headers)
          .withRequestTimeout(10 seconds)
    }
  }

  @Provides
  @Named("internal-http-client")
  def provideInternalHttpClient(
                                 auditConnector: HttpAuditing,
                                 wsClient: WSClient,
                                 actorSystem: ActorSystem
                               ): HttpClient = {
    new DefaultHttpClient(playConfig, auditConnector, wsClient, actorSystem) {
      override def buildRequest(
                                 url: String,
                                 headers: Seq[(String, String)] = Seq.empty
                               ): ws.WSRequest =
        super
          .buildRequest(url, headers)
          .withRequestTimeout(2 seconds)
    }
  }

  @Provides
  @Singleton
  def provideAuditClient(auditConnector: AuditConnector): AuditService = {
    val appName = playConfig.getOptional[String]("appName").getOrElse("APP NAME NOT SET")
    val appVersion = BuildInfo.version

    val auditor = Audit(appName, auditConnector)

    val auditClientSeam = new AuditServiceSeam(auditor)
    new AuditServiceImpl(auditClientSeam, appName, appVersion)
  }
}
