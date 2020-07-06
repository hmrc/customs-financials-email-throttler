/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.emailthrottler.controllers

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.customs.financials.emailthrottler.services.EmailQueue

import scala.concurrent.ExecutionContext.Implicits.global
//noinspection TypeAnnotation
class EmailThrottlerControllerSpec extends PlaySpec with MockitoSugar {

  class EmailThrottlerScenario() {
    val requestBody = Json.parse(
      """{
        | "to": ["email1@example.co.uk", "email1@example.co.uk"],
        | "templateId": "template_for_duty_deferment_email",
        | "parameters": {
        |   "param1": "value1",
        |   "param2": "value2"
        | },
        | "force": false,
        | "eventUrl": "event.url.co.uk",
        | "onSendUrl": "on.send.url.co.uk"
        |}""".stripMargin)
    val fakeRequest = FakeRequest("POST", "/", FakeHeaders(), requestBody)
    val mockEmailQueue = mock[EmailQueue]

    val controller = new EmailThrottlerController(mockEmailQueue, Helpers.stubControllerComponents())
  }

  "the controller" should {

    "handle enqueue request" in new EmailThrottlerScenario {
      val result = controller.enqueueEmail()(fakeRequest)
      status(result) mustBe Status.ACCEPTED
    }

    "ask EmailQueue service to store emails" in new EmailThrottlerScenario {
      await(controller.enqueueEmail()(fakeRequest))
      verify(mockEmailQueue).enqueueJob(ArgumentMatchers.any())
    }

    "respond Bad Request to invalid request" in new EmailThrottlerScenario {
      val invalidRequestBody= Json.parse("{}")
      val invalidRequest = FakeRequest("POST", "/", FakeHeaders(), invalidRequestBody)
      val result = controller.enqueueEmail()(invalidRequest)
      status(result) mustBe Status.BAD_REQUEST
    }

  }

}
