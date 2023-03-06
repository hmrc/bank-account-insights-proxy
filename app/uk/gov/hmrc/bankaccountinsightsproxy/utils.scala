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

import play.api.libs.json.{JsObject, JsPath, Json, JsonValidationError}

import scala.collection.Seq

object utils {
  object json {
    def simplifyJsonErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): JsObject =
      errors.foldLeft(Json.obj()) { case (obj, (path, errorsForPath)) =>
        obj ++ Json.obj(path.toString -> errorsForPath.map(_.message))
      }
  }
}
