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

package uk.gov.hmrc.customs.financials.emailthrottler.services

import java.time.{OffsetDateTime, ZoneOffset}

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.scalatest.WordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment, Mode}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.domain.{EmailRequest, SendEmailJob}
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.concurrent.Future

//noinspection TypeAnnotation
class EmailJobHandlerSpec extends WordSpec with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global

  class MockedEmailJobHandlerScenario() {

    val sendEmailJob = SendEmailJob(
      BSONObjectID.generate,
      EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
      OffsetDateTime.of(2019,10,8,15,1,0,0,ZoneOffset.UTC),
      processing = true
    )

    val mockEmailQueue = mock[EmailQueue]
    when(mockEmailQueue.nextJob).thenReturn(Future.successful(Some(sendEmailJob)))
    when(mockEmailQueue.deleteJob(ArgumentMatchers.any())).thenReturn(Future.successful(()))

    val mockEmailNotificationService = mock[EmailNotificationService]
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

      "integration" in {
        val env           = Environment.simple()
        val configuration = Configuration.load(env)
        val serviceConfig = new ServicesConfig(configuration, new RunMode(configuration, Mode.Dev))
        val appConfig     = new AppConfig(configuration, serviceConfig, env)
        val dateTimeService = new DateTimeService
        val reactiveMongoComponent = new ReactiveMongoComponent {
          val mongoUri = "mongodb://127.0.0.1:27017/test-customs-email-throttler"
          override def mongoConnector: MongoConnector = MongoConnector(mongoUri)
        }
        val metricsReporter = mock[MetricsReporterService]
        val emailQueue = new EmailQueue(reactiveMongoComponent, appConfig, dateTimeService, metricsReporter)
        await(emailQueue.removeAll())

        val emailRequests = Seq(
          EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None)
        )
        emailRequests.foreach(request => await(emailQueue.enqueueJob(request)))

        val mockEmailNotificationService = mock[EmailNotificationService]
        when(mockEmailNotificationService.sendEmail(ArgumentMatchers.any())).thenReturn(Future.successful(true))
        val service = new EmailJobHandler(emailQueue, mockEmailNotificationService)

        await(service.processJob())
        await(service.processJob())

        reactiveMongoComponent.mongoConnector.close()
      }
    }

  }
}
