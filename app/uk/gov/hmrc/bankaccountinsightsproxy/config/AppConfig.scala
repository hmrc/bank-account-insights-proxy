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

package uk.gov.hmrc.bankaccountinsightsproxy
package config


import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import java.nio.charset.StandardCharsets
import java.util.Base64

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig):
  val bankAccountInsightsBaseUrl: String = servicesConfig.baseUrl("bank-account-insights")
  val bankAccountInsightsAuthToken = s"Basic ${createAuth("bank-account-insights.authToken")}"

  private def createAuth(authTokenKey: String) =
    AppConfig.createAuth(
      config.get[String]("appName"),
      servicesConfig.getConfString(authTokenKey, "invalid-token"))

object AppConfig:
  def createAuth(appName: String, authToken: String): String =
    Base64.getEncoder.encodeToString(s"$appName:$authToken".getBytes(StandardCharsets.UTF_8))
