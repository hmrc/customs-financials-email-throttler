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

package uk.gov.hmrc.customs.financials.emailthrottler.models

import play.api.libs.json._
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.customs.financials.emailthrottler.utils.TestData.{
  DAY_15, EMPTY_STRING, HOUR_10, MINUTES_10,
  MONTH_3, SECONDS_10, YEAR_2024
}

import java.time.LocalDateTime

class SendEmailJobSpec extends SpecBase {

  "Reads" should {
    "generate correct output" in new Setup {

      import SendEmailJob.formatSendEmailJob

      Json.fromJson(Json.parse(sendEmailJsValueString)) mustBe JsSuccess(sendEmailJob)
    }
  }

  "Writes" should {
    "generate correct output" in new Setup {
      Json.toJson(sendEmailJob) mustBe Json.parse(sendEmailJsValueString)
    }
  }

  trait Setup {
    val id = "test_id"
    val emailReq: EmailRequest = EmailRequest(List.empty, EMPTY_STRING, Map.empty, force = false, None, None)
    val date: LocalDateTime = LocalDateTime.of(YEAR_2024, MONTH_3, DAY_15, HOUR_10, MINUTES_10, SECONDS_10)

    val sendEmailJob: SendEmailJob = SendEmailJob(id, emailReq, processing = true, date)

    val sendEmailJsValueString: String =
      """{"_id":"test_id",
        |"emailRequest":{
        |"to":[],
        |"templateId":"",
        |"parameters":{},
        |"force":false},
        |"processing":true,
        |"lastUpdated":{"$date":{"$numberLong":"1710497410000"}
        |}
        |}""".stripMargin
  }
}
