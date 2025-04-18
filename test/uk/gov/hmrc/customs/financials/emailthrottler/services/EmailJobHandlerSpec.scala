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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailRequest, SendEmailJob}
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.mongo.play.PlayMongoComponent

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class EmailJobHandlerSpec extends SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  class MockedEmailJobHandlerScenario() {

    val tdYear       = 2019
    val tdMonth      = 10
    val tdDayOfMonth = 8
    val tdHour       = 15
    val tdMinute     = 1
    val tdSecond     = 0
    val tdNanoSecond = 0

    val sendEmailJob: SendEmailJob = SendEmailJob(
      UUID.randomUUID().toString,
      EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
      processing = true,
      LocalDateTime.of(tdYear, tdMonth, tdDayOfMonth, tdHour, tdMinute, tdSecond, tdNanoSecond)
    )

    val mockEmailQueue: EmailQueue = mock(classOf[EmailQueue])
    when(mockEmailQueue.nextJob).thenReturn(Future.successful(Some(sendEmailJob)))
    when(mockEmailQueue.deleteJob(ArgumentMatchers.any())).thenReturn(Future.successful(true))

    val mockEmailNotificationService: EmailNotificationService = mock(classOf[EmailNotificationService])
    when(mockEmailNotificationService.sendEmail(ArgumentMatchers.any())).thenReturn(Future.successful(true))

    val service = new EmailJobHandler(mockEmailQueue, mockEmailNotificationService)
  }

  "EmailJobHandlerSpec" should {
    "process job" should {

      "fetch job from email queue" in new MockedEmailJobHandlerScenario {
        await(service.processJob())
        verify(mockEmailQueue).nextJob
      }

      "ask email notification service to send email" in new MockedEmailJobHandlerScenario {
        await(service.processJob())
        verify(mockEmailNotificationService).sendEmail(ArgumentMatchers.any())
      }

      "ask email queue to delete completed job" in new MockedEmailJobHandlerScenario {
        await(service.processJob())
        verify(mockEmailQueue).deleteJob(ArgumentMatchers.any())
      }

      "housekeeping " in new MockedEmailJobHandlerScenario {
        service.houseKeeping()
        verify(mockEmailQueue).resetProcessing
      }

      "integration" in {
        val appConfig                = mock(classOf[AppConfig])
        val mockConfiguration        = mock(classOf[Configuration])
        val mockApplicationLifeCycle = mock(classOf[ApplicationLifecycle])

        when(mockConfiguration.get(ArgumentMatchers.eq("mongodb.uri"))(any))
          .thenReturn("mongodb://127.0.0.1:27017/test-customs-email-throttler")

        when(mockConfiguration.get[FiniteDuration]("hmrc.mongo.init.timeout"))
          .thenReturn(5.seconds)

        val reactiveMongoComponent: PlayMongoComponent =
          new PlayMongoComponent(mockConfiguration, lifecycle = mockApplicationLifeCycle)

        val metricsReporter     = mock(classOf[MetricsReporterService])
        val mockDateTimeService = mock(classOf[DateTimeService])
        val emailQueue          = new EmailQueue(reactiveMongoComponent, mockDateTimeService, appConfig, metricsReporter)

        when(mockDateTimeService.getLocalDateTime).thenCallRealMethod()

        val emailRequests = Seq(
          EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None)
        )

        emailRequests.foreach(request => await(emailQueue.enqueueJob(request)))

        val mockEmailNotificationService = mock(classOf[EmailNotificationService])
        when(mockEmailNotificationService.sendEmail(ArgumentMatchers.any())).thenReturn(Future.successful(true))
        val service                      = new EmailJobHandler(emailQueue, mockEmailNotificationService)

        await(service.processJob())
        await(service.processJob())

        reactiveMongoComponent.client.close()
      }
    }
  }
}
