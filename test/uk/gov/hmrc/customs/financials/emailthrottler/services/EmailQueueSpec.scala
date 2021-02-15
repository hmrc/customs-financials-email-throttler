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

import java.time.{Instant, OffsetDateTime, ZoneOffset}

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{spy, verify, when}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.domain.{EmailAddress, EmailRequest}
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.Future

//noinspection TypeAnnotation
class EmailQueueSpec extends WordSpec with MockitoSugar with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach with MustMatchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  val mockAppConfig = mock[AppConfig]
  val mockDateTimeService = mock[DateTimeService]
  when(mockDateTimeService.getTimeStamp).thenCallRealMethod()

  val reactiveMongoComponent = new ReactiveMongoComponent {
    val mongoUri = "mongodb://127.0.0.1:27017/test-customs-email-throttler"

    override def mongoConnector: MongoConnector = MongoConnector(mongoUri)
  }

  val metricsReporter = mock[MetricsReporterService]
  val emailQueue = new EmailQueue(reactiveMongoComponent, mockAppConfig, mockDateTimeService, metricsReporter)

  override def beforeEach: Unit = {
    await(emailQueue.removeAll())
  }

  "EmailAddress" should {

    "obfuscate toString" in {
      val emailAddress = EmailAddress("test@nowhere")

      assert(emailAddress.toString() == "************")
    }
  }

  "EmailQueue" should {

    "enqueue email request" should {

      "insert email job into collection" in {
        val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
        val spyEmailQueue = spy(emailQueue)

        spyEmailQueue.enqueueJob(emailRequest)

        verify(spyEmailQueue).insert(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "insert multiple email job with same time stamp into collection" in {
        val timeStamp = OffsetDateTime.ofInstant( Instant.now() , ZoneOffset.UTC)
        when(mockDateTimeService.getTimeStamp).thenReturn(timeStamp)
        val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
        val eventualResults = (1 to 10).map(_ => emailQueue.enqueueJob(emailRequest))
        await(Future.sequence(eventualResults))
      }

      "delete email job by id" in {
        val spyEmailQueue = spy(emailQueue)

        spyEmailQueue.deleteJob(BSONObjectID.generate())

        verify(spyEmailQueue).removeById(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "audit requests and insert result" in {
        pending
      }

      "audit queue length" in {
        pending
      }

      "get oldest, not processed, send email job" in {
        when(mockDateTimeService.getTimeStamp)
          .thenReturn(OffsetDateTime.of(2019,10,8,15,1,0,0,ZoneOffset.UTC))
          .thenReturn(OffsetDateTime.of(2019,10,8,15,2,0,0,ZoneOffset.UTC))
          .thenReturn(OffsetDateTime.of(2019,10,8,15,3,0,0,ZoneOffset.UTC))

        val emailRequests = Seq(
          EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None)
        )
        await(Future.sequence(emailRequests.map(emailQueue.enqueueJob)))

        val expectedEmailRequest = EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None)
        val job = await(emailQueue.nextJob)
        job.map(_.emailRequest) mustBe Some(expectedEmailRequest)

        val expectedEmailRequest2 = EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None)
        val job2 = await(emailQueue.nextJob)
        job2.map(_.emailRequest) mustBe Some(expectedEmailRequest2)
      }

    }

  }

}
