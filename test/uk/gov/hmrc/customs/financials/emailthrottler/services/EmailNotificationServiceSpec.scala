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

package uk.gov.hmrc.customs.financials.emailthrottler.services

import org.mockito.Mockito.{mock, when}
import org.mockito.ArgumentMatchers.any
import play.api
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.{Application, inject}
import uk.gov.hmrc.customs.financials.emailthrottler.models.*
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.*

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class EmailNotificationServiceSpec extends SpecBase {

  "sendEmail" should {
    "send the email request" in new Setup {
      running(app) {
        val request: EmailRequest = EmailRequest(List(EmailAddress("toAddress")), "templateId")

        when(mockRequestBuilder.withBody(any[EmailRequest]())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(Status.ACCEPTED, "")))

        when(mockHttpClient.post(any[URL]())(any())).thenReturn(mockRequestBuilder)

        await(emailNotificationService.sendEmail(request)) mustBe true
      }
    }

    "fail to send the email request" in new Setup {
      running(app) {
        val request: EmailRequest = EmailRequest(List(EmailAddress("incorrectEmailAddress")), "templateId")

        when(mockRequestBuilder.withBody(any[EmailRequest]())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(Status.BAD_REQUEST, "")))

        when(mockHttpClient.post(any[URL]())(any())).thenReturn(mockRequestBuilder)

        await(emailNotificationService.sendEmail(request)) mustBe false
      }
    }

    "recover from exception" in new Setup {
      running(app) {
        val request: EmailRequest = EmailRequest(List(EmailAddress("incorrectEmailAddress")), "templateId")

        when(mockRequestBuilder.withBody(any[EmailRequest]())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new HttpException("Internal server error", Status.INTERNAL_SERVER_ERROR)))

        when(mockHttpClient.post(any[URL]())(any())).thenReturn(mockRequestBuilder)

        await(emailNotificationService.sendEmail(request)) mustBe false
      }
    }
  }

  trait Setup {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    implicit val mockHttpClient: HttpClientV2 = mock(classOf[HttpClientV2])
    implicit val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])

    val app: Application = new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[HttpClientV2].toInstance(mockHttpClient),
        api.inject.bind[RequestBuilder].toInstance(mockRequestBuilder))
      .build()

    val emailNotificationService: EmailNotificationService = app.injector.instanceOf[EmailNotificationService]
  }
}