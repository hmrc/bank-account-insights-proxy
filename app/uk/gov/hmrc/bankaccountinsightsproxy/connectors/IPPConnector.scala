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

package uk.gov.hmrc.bankaccountinsightsproxy.connectors

import akka.http.scaladsl.model.MediaTypes
import play.api.http.{HeaderNames, Status}
import uk.gov.hmrc.bankaccountinsightsproxy.config.AppConfig
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest
import uk.gov.hmrc.bankaccountinsightsproxy.model.request.InsightsRequest.implicits._
import uk.gov.hmrc.bankaccountinsightsproxy.model.response.IPPResponse
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient, HttpResponse}

import java.net.URL
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IPPConnector @Inject()(
    appConfig: AppConfig,
    @Named("internal-http-client") httpClient: HttpClient
) {
  private val bankAccountInsightsUrl = s"${appConfig.bankAccountInsightsBaseUrl}/ipp"
  private val authorization = appConfig.bankAccountInsightsAuthToken

  def ipp(insightsRequest: InsightsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[String, IPPResponse]] = {
    // Ensure we REPLACE the auth header instead of potentially adding a second one by using the headers parameter no the POST method
    doPost(insightsRequest)(ec, hc.copy(authorization = Some(Authorization(authorization))))
  }

  private def doPost(bankAccountInsightsRequest: InsightsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[String, IPPResponse]] = {
    import IPPResponse.Implicits._
    httpClient.POST[InsightsRequest, HttpResponse](
      url = new URL(bankAccountInsightsUrl),
      body = bankAccountInsightsRequest,
      headers = Seq(HeaderNames.CONTENT_TYPE -> MediaTypes.`application/json`.toString())
    ) map {
      case r if r.status == Status.OK =>
        val res = r.json.as[IPPResponse]
        Right(res)
    } recover {
      case e: Throwable => Left(e.getMessage)
    }
  }
}
