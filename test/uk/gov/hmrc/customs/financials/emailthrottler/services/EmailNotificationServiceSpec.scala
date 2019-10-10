/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.customs.financials.emailthrottler.domain.{AuditModel, EmailRequest}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.MustMatchers
import play.api.http.Status
import play.api.libs.json.{JsString, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

//import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class EmailNotificationServiceSpec extends MockAuditingService with MockitoAnswerSugar with MustMatchers with FutureAwaits with DefaultAwaitTimeout {

  trait EmailNotificationServiceScenario {
    implicit val mockAppConfig = mock[AppConfig]
    implicit val mockHttpClient: HttpClient = mock[HttpClient]

    val mockMetricsReporterService = mock[MetricsReporterService]
    when(mockMetricsReporterService.withResponseTimeLogging(any())(any())(any()))
      .thenAnswer((i: InvocationOnMock) => {i.getArgument[Future[JsString]](1)})


    implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val emailNotificationService = new EmailNotificationService(mockHttpClient, mockMetricsReporterService, mockAuditingService)
  }

  // We're testing emails
  FeatureSwitch.EmailNotifications.enable()

  "sendEmail" should {
    "send the email request" in new EmailNotificationServiceScenario {

      val request = EmailRequest(List("toAddress"), "templateId", Map.empty, false, Some("url"), Some("url"))

      when[Future[HttpResponse]](mockHttpClient.POST(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(Status.ACCEPTED)))
      await(emailNotificationService.sendEmail(request)) mustBe true
    }

    "fail to send the email request" in new EmailNotificationServiceScenario {
      val request = EmailRequest(List("incorrectEmailAddress"), "templateId", Map.empty, false, Some("url"), Some("url"))

      when[Future[HttpResponse]](mockHttpClient.POST(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(Status.BAD_REQUEST)))

      await(emailNotificationService.sendEmail(request)) mustBe false
    }

    "recover from exception" in new EmailNotificationServiceScenario {
      val request = EmailRequest(List("incorrectEmailAddress"), "templateId", Map.empty, false, Some("url"), Some("url"))

      when[Future[HttpResponse]](mockHttpClient.POST(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new HttpException("Internal server error", Status.INTERNAL_SERVER_ERROR)))

      await(emailNotificationService.sendEmail(request)) mustBe false
    }


    "audit the request" in new EmailNotificationServiceScenario  {
      val request = EmailRequest(List("toAddress"), "templateId", Map.empty, false, Some("url"), Some("url"))
      val expectedAuditRequest = Json.toJson(request)

      when[Future[HttpResponse]](mockHttpClient.POST(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(Status.ACCEPTED)))

      await(emailNotificationService.sendEmail(request))

      import emailNotificationService.{AUDIT_EMAIL_REQUEST, AUDIT_TYPE}
      verifyAudit(AuditModel(AUDIT_EMAIL_REQUEST, expectedAuditRequest, AUDIT_TYPE))
    }
  }
}
