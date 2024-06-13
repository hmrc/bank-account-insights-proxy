/*
 * Copyright 2024 HM Revenue & Customs
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
package controllers

import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import play.api.libs.json.Json.*
import play.api.mvc.Results.{BadRequest, NotFound, Status}
import play.api.mvc.{RequestHeader, Result}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.{ErrorResponse, JsonErrorHandler}
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CIRJsonErrorHandler @Inject()(
                                  auditConnector: AuditConnector,
                                  httpAuditEvent: HttpAuditEvent,
                                  configuration : Configuration
                                )(implicit ec: ExecutionContext
                                ) extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration)(ec):

  import httpAuditEvent.dataEvent

  private val logger = Logger(getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = 
    implicit val headerCarrier: HeaderCarrier = hc(request)
    val result = statusCode match
      case NOT_FOUND =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ResourceNotFound",
            transactionName = "Resource Endpoint Not Found",
            request         = request,
            detail          = Map.empty
          )
        )
        NotFound(toJson(ErrorResponse(NOT_FOUND, "URI not found", requested = Some(request.path))))

      case BAD_REQUEST =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ServerValidationError",
            transactionName = "Request bad format exception",
            request         = request,
            detail          = Map.empty
          )
        )
        def constructErrorMessage(input: String): String = 
          val unrecognisedTokenJsonError = "^Invalid Json: Unrecognized token '(.*)':.*".r
          val invalidJson                = "^(?s)Invalid Json:.*".r
          val jsonValidationError        = "^Json validation error.*".r
          val booleanParsingError        = "^Cannot parse parameter .* as Boolean: should be true, false, 0 or 1$".r
          val missingParameterError      = "^Missing parameter:.*".r
          val characterParseError        = "^Cannot parse parameter .* with value '(.*)' as Char: .* must be exactly one digit in length.$".r
          val parameterParseError        = "^Cannot parse parameter .* as .*: For input string: \"(.*)\"$".r
          input match {
            case unrecognisedTokenJsonError(_)
                 | invalidJson()
                 | jsonValidationError()
                 | booleanParsingError()
                 | missingParameterError()                  => "bad request, cause: invalid json"
            case characterParseError(toBeRedacted)        => input.replace(toBeRedacted, "REDACTED")
            case parameterParseError(toBeRedacted)        => input.replace(toBeRedacted, "REDACTED")
            case _                                        => "bad request, cause: REDACTED"
          }
        val msg = constructErrorMessage(message)

        BadRequest(toJson(ErrorResponse(BAD_REQUEST, msg)))

      case _ =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ClientError",
            transactionName = s"A client error occurred, status: $statusCode",
            request         = request,
            detail          = Map.empty
          )
        )

        val msg =
          if (suppress4xxErrorMessages) "Other error"
          else message

        Status(statusCode)(toJson(ErrorResponse(statusCode, msg)))
    
    Future.successful(result)
  end onClientError
    

