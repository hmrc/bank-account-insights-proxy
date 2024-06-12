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

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject

class ExtendedServicesConfig @Inject()(configuration: Configuration) extends ServicesConfig(configuration):

  def getRequiredBase64EncodedConfString(confKey: String): String =
    new String(Base64.getDecoder.decode(getRequiredConfString(confKey)), StandardCharsets.UTF_8)

  def getRequiredConfString(confKey: String): String =
    getConfString(confKey, throw new RuntimeException(s"Could not find config $confKey"))

  def getRequiredConfBoolean(confKey: String): Boolean =
    getConfBool(confKey, throw new RuntimeException(s"Could not find config $confKey"))

  def getRequiredIntSeq(confKey: String): Seq[Int] =
    getIntSeq(confKey, throw new RuntimeException(s"Could not find config $confKey"))

  def getRequiredStringSeq(confKey: String): Seq[String] = {
    getStringSeq(confKey, throw new RuntimeException(s"Could not find config $confKey"))
  }

  override def getConfString(confKey: String, defString: => String): String =
    configuration.getOptional[String](s"$rootServices.$confKey")
          .getOrElse(defString)

  def getIntSeq(confKey: String, defaults: => Seq[Int]): Seq[Int] =
    configuration.getOptional[Seq[Int]](s"$rootServices.$confKey")
          .getOrElse(defaults)

  def getStringSeq(confKey: String, defaults: => Seq[String]): Seq[String] =
    configuration.getOptional[Seq[String]](s"$rootServices.$confKey")
          .getOrElse(defaults)
