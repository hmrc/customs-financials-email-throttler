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

package uk.gov.hmrc.customs.financials.emailthrottler.utils

import play.api.libs.json.*

class JsonFormatUtilsSpec extends SpecBase {

  "stringFormat" should {

    "read the JsValue correctly" in new Setup {
      Json.fromJson(JsString(testValue)).get mustBe testObj
    }

    "write the object correctly" in new Setup {
      Json.toJson(testObj) mustBe JsString(testValue)
    }

  }

  trait Setup {
    implicit val format: Format[TestClass] = JsonFormatUtils.stringFormat[TestClass](TestClass.apply)(_.value)
    val testValue                          = "test"
    val testObj: TestClass                 = TestClass(testValue)
  }
}

case class TestClass(value: String)
