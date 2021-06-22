/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.Mockito.{spy, when}
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import play.api
import play.api.{Application, inject}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailAddress, EmailRequest, SendEmailJob}
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailQueueSpec extends SpecBase with BeforeAndAfterEach {

  "EmailAddress" should {
    "obfuscate toString" in {
      val emailAddress = EmailAddress("test@nowhere")
      assert(emailAddress.toString() == "************")
    }
  }

  "EmailQueue" should {
    "insert email job into collection" in new Setup {
      running(app) {
        when(mockDateTimeService.getLocalDateTime).thenCallRealMethod()
        val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
        val spyEmailQueue = spy(emailQueue)
        val result: Boolean = await(spyEmailQueue.enqueueJob(emailRequest))
        result mustBe true

        await(dropData)
      }
    }

    "insert multiple email job with same time stamp into collection" in new Setup {
      running(app) {
        when(mockDateTimeService.getLocalDateTime).thenCallRealMethod()
        val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
        val eventualResults = (1 to 10).map(_ => emailQueue.enqueueJob(emailRequest))
        await(Future.sequence(eventualResults))

        await(dropData)
      }
    }

    "delete email job by id" in new Setup {
      running(app) {
        val spyEmailQueue = spy(emailQueue)

        val result = await(spyEmailQueue.deleteJob(UUID.randomUUID().toString))

        result mustBe true

        await(dropData)
      }
    }

    "send all email jobs with processing set to false" in {
      val app: Application = new GuiceApplicationBuilder().build()
      val emailQueue: EmailQueue = app.injector.instanceOf[EmailQueue]

      val job1 = SendEmailJob("id-1", EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None), processing = false, LocalDateTime.now().minusMinutes(20))
      val job2 = SendEmailJob("id-2", EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None), processing = false, LocalDateTime.now().minusMinutes(10))

      running(app) {

        await(for{
          _ <- emailQueue.collection.insertOne(job1).toFuture()
          _ <- emailQueue.collection.insertOne(job2).toFuture()
          result1 <- emailQueue.nextJob
          result2 <- emailQueue.nextJob
          result3 <- emailQueue.nextJob
          _ <- emailQueue.collection.drop().toFuture()
        } yield {
          result1.nonEmpty mustBe true
          result2.nonEmpty mustBe true
          result3.nonEmpty mustBe false
        })
      }

    }

    "reset the processing flag for emails which are older than maximum age (30 minutes)" in {

      val app: Application = new GuiceApplicationBuilder().build()
      val emailQueue: EmailQueue = app.injector.instanceOf[EmailQueue]

      val job1 = SendEmailJob("id-1", EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None), processing = true, LocalDateTime.now().minusMinutes(15))
      val job2 = SendEmailJob("id-2", EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None), processing = true, LocalDateTime.now().minusHours(2))
      val job3 = SendEmailJob("id-3", EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None), processing = true, LocalDateTime.now().minusMinutes(15))
      val job4 = SendEmailJob("id-4", EmailRequest(List.empty, "id_4", Map.empty, force = false, None, None), processing = true, LocalDateTime.now().minusHours(2))
      val job5 = SendEmailJob("id-5", EmailRequest(List.empty, "id_5", Map.empty, force = false, None, None), processing = true, LocalDateTime.now().minusMinutes(15))

      running(app) {
        await(emailQueue.collection.insertMany(Seq(job1, job2, job3, job4, job5)).toFuture())
        await(emailQueue.resetProcessing)
        val results = await(emailQueue.collection.find(Filters.equal("processing", true)).toFuture())
        val expectedResult = Seq(job1, job3, job5)
        results mustBe expectedResult
        await(emailQueue.collection.drop().toFuture())
      }
    }
  }

  trait Setup {
    val mockDateTimeService: DateTimeService = mock[DateTimeService]
    val app: Application = new GuiceApplicationBuilder()
      .overrides(api.inject.bind[DateTimeService].toInstance(mockDateTimeService))
      .build()
    val emailQueue: EmailQueue = app.injector.instanceOf[EmailQueue]
    await(dropData)

    def dropData: Future[Unit] = {
      emailQueue.collection.drop().toFuture().map(_ => ())
    }
  }

}
