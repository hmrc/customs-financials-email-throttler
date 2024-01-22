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

import org.mockito.Mockito.{mock, spy, when}
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
        running(app){
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

      "delete email job by id" in new Setup  {
        running(app) {
          val spyEmailQueue = spy(emailQueue)

          val result = await(spyEmailQueue.deleteJob(UUID.randomUUID().toString))

          result mustBe true

          await(dropData)
        }
      }

    "send all email jobs with processing set to false" in {
      val mockScheduler = mock(classOf[Scheduler])

      val app: Application = new GuiceApplicationBuilder()
        .overrides(api.inject.bind[Scheduler].toInstance(mockScheduler))
        .build()

      val emailQueue: EmailQueue = app.injector.instanceOf[EmailQueue]

      val oldestJob = SendEmailJob("id-1", EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None), processing = false, LocalDateTime.of(2021,4,10,1,0,0))
      val latestJob = SendEmailJob("id-2", EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None), processing = false, LocalDateTime.of(2021,4,10,5,0,0))

      running(app) {
        await(for {
          _ <- emailQueue.collection.insertMany(Seq(latestJob,oldestJob)).toFuture()
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

      "reset the processing flag for emails which are older than maximum age" in new Setup  {

        val tdYear = 2021
        val tdMonth = 4
        val tdDayOfMonth = 7
        val tdHour = 15
        val tdVal0 = 0
        val tdVal1 = 1
        val tdVal28 = 28
        val tdVal30 = 30
        val tdVal31 = 31
        val tdVal59 = 59

        when(mockDateTimeService.getLocalDateTime)
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal0,tdVal0,tdVal0))
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal1,tdVal0,tdVal0))
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal28,tdVal0,tdVal0))
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal30,tdVal0,tdVal0))
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal31,tdVal0,tdVal0))
          .thenReturn(LocalDateTime.of(tdYear,tdMonth,tdDayOfMonth,tdHour,tdVal59,tdVal0,tdVal0))

        val emailRequests = Seq(
          EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_4", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_5", Map.empty, force = false, None, None)
        )
        running(app) {
          await(Future.sequence(emailRequests.map((emailRequest: EmailRequest) => emailQueue.enqueueJob(emailRequest))))
          emailRequests.map(_ => await(emailQueue.nextJob))
          val emailQueueCollection = emailQueue.collection
          val countAllTrue: Long = await(emailQueueCollection.countDocuments(
                                    filter = Filters.equal("processing", true)).toFuture().map(s => s))
          countAllTrue must be(emailRequests.size)

          await(emailQueue.resetProcessing)

          val resetCount: Long = await(emailQueueCollection.countDocuments(
            filter = Filters.equal("processing", false)).toFuture().map(s => s))
          resetCount must be(3)

          await(dropData)
        }
      }
  }

  trait Setup{
    val mockAppConfig: AppConfig = mock(classOf[AppConfig])
    val mockDateTimeService: DateTimeService = mock(classOf[DateTimeService])
    val app = new GuiceApplicationBuilder()
      .overrides(api.inject.bind[DateTimeService].toInstance(mockDateTimeService))
      .build()
    val emailQueue = app.injector.instanceOf[EmailQueue]
    await(dropData)

    def dropData:Future[Unit] = {
      emailQueue.collection.drop().toFuture().map(_ => ())
    }


  }

}
