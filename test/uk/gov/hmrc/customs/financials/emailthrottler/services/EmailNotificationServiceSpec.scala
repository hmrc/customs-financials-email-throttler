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

import org.mockito.ArgumentMatchers.{eq => is, _}
import org.mockito.Mockito.{mock, when}
import org.mockito.invocation.InvocationOnMock
import play.api.http.Status
import play.api.libs.json.JsString
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models._
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContext}

class EmailNotificationServiceSpec extends SpecBase {

  "sendEmail" should {
    "send the email request" in new EmailNotificationServiceScenario {

      val request: EmailRequest = EmailRequest(List(EmailAddress("toAddress")), "templateId")

      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any[(String, String)]())).thenReturn(mockRequestBuilder)

      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(Status.ACCEPTED, "")))

      when(mockHttpClient.put(any)(any)).thenReturn(mockRequestBuilder)

      /*when(mockHttpClient.post(any())).thenReturn(Future.successful(HttpResponse(Status.ACCEPTED, "")))

     when[Future[HttpResponse]](mockHttpClient.POST(any(), is(request), any())(any(), any(), any(), any()))
       .thenReturn(Future.successful(HttpResponse(Status.ACCEPTED, "")))*/

      await(emailNotificationService.sendEmail(request)) mustBe true
    }

   /* "fail to send the email request" in new EmailNotificationServiceScenario {
      val request: EmailRequest = EmailRequest(List(EmailAddress("incorrectEmailAddress")), "templateId")

      when[Future[HttpResponse]](mockHttpClient.POST(any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(Status.BAD_REQUEST, "")))

      await(emailNotificationService.sendEmail(request)) mustBe false
    }

    "recover from exception" in new EmailNotificationServiceScenario {
      val request: EmailRequest = EmailRequest(List(EmailAddress("incorrectEmailAddress")), "templateId")

      when[Future[HttpResponse]](mockHttpClient.POST(any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new HttpException("Internal server error", Status.INTERNAL_SERVER_ERROR)))

      await(emailNotificationService.sendEmail(request)) mustBe false
    }*/
  }

  trait EmailNotificationServiceScenario {
    implicit val mockAppConfig: AppConfig = mock(classOf[AppConfig])
    implicit val mockHttpClient: HttpClientV2 = mock(classOf[HttpClientV2])
    implicit val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])

    val mockMetricsReporterService: MetricsReporterService = mock(classOf[MetricsReporterService])
    when(mockMetricsReporterService.withResponseTimeLogging(any())(any())(any()))
      .thenAnswer((i: InvocationOnMock) => {
        i.getArgument[Future[JsString]](1)
      })

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val emailNotificationService = new EmailNotificationService(mockHttpClient, mockMetricsReporterService)
  }
}
