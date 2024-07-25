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

package uk.gov.hmrc.customs.financials.emailthrottler.controllers

import org.mockito.Mockito.{mock,verify}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailAddress, EmailRequest}
import uk.gov.hmrc.customs.financials.emailthrottler.services.EmailQueue
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailThrottlerControllerSpec extends SpecBase {

  "the controller" should {
    "handle enqueue request" in new Setup {
      val result: Future[Result] = controller.enqueueEmail()(fakeRequest)
      status(result) mustBe Status.ACCEPTED
    }

    "ask EmailQueue service to store emails" in new Setup {
      await(controller.enqueueEmail()(fakeRequest))
      val expectedResult: EmailRequest = EmailRequest(
        List(
          EmailAddress("email1@example.co.uk"),
          EmailAddress("email1@example.co.uk")
        ),
        templateId = "template_for_duty_deferment_email",
        parameters = Map("param1" -> "value1", "param2" -> "value2"),
        enrolment = Some("HMRC-CUS-ORG~EORINumber~testEori"),
        eventUrl = Some("event.url.co.uk"),
        onSendUrl = Some("on.send.url.co.uk")
      )
      verify(mockEmailQueue).enqueueJob(expectedResult)
    }
  }

  trait Setup {
    val requestBody: JsValue = Json.parse(
      """{
        | "to":[{"address":"email1@example.co.uk"},{"address":"email1@example.co.uk"}],
        | "templateId": "template_for_duty_deferment_email",
        | "parameters": {
        |   "param1": "value1",
        |   "param2": "value2"
        | },
        | "force": false,
        | "enrolment": "testEori",
        | "eventUrl": "event.url.co.uk",
        | "onSendUrl": "on.send.url.co.uk"
        |}""".stripMargin)

    val fakeRequest: FakeRequest[EmailRequest] = FakeRequest("POST", "/", FakeHeaders(), requestBody.as[EmailRequest])
    val mockEmailQueue: EmailQueue = mock(classOf[EmailQueue])

    val controller: EmailThrottlerController =
      new EmailThrottlerController(mockEmailQueue, Helpers.stubControllerComponents())
  }
}
