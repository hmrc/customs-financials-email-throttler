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
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailAddress, EmailRequest}
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase

import java.time.LocalDateTime
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
          val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
          val spyEmailQueue = spy(emailQueue)
          val result: Boolean = await(spyEmailQueue.enqueueJob(emailRequest))
          result mustBe true
        }
      }

      "insert multiple email job with same time stamp into collection" in new Setup {
        running(app) {
          val emailRequest = EmailRequest(List.empty, "", Map.empty, force = false, None, None)
          val eventualResults = (1 to 10).map(_ => emailQueue.enqueueJob(emailRequest))
          await(Future.sequence(eventualResults))
        }
      }

      "delete email job by id" in new Setup  {
        running(app) {
          val spyEmailQueue = spy(emailQueue)

          val result = await(spyEmailQueue.deleteJob(UUID.randomUUID().toString))

          result mustBe true
        }
      }

      "get oldest, not processed, send email job" in new Setup  {
        val emailRequests = Seq(
          EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None),
          EmailRequest(List.empty, "id_3", Map.empty, force = false, None, None)
        )
        running(app) {
          await(Future.sequence(emailRequests.map((emailRequest: EmailRequest) => emailQueue.enqueueJob(emailRequest))))

          val expectedEmailRequest = EmailRequest(List.empty, "id_1", Map.empty, force = false, None, None)
          val job = await(emailQueue.nextJob)
          job.map(_.emailRequest) mustBe Some(expectedEmailRequest)

          val expectedEmailRequest2 = EmailRequest(List.empty, "id_2", Map.empty, force = false, None, None)
          val job2 = await(emailQueue.nextJob)
          job2.map(_.emailRequest) mustBe Some(expectedEmailRequest2)

        }
      }

      "reset the processing flag for emails which are older than maximum age" in new Setup  {
        when(mockAppConfig.emailMaxAgeMins).thenReturn(0)

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

          val countAllTrue: Long = await(emailQueue.countDocuments(true))
          countAllTrue must be(emailRequests.size)

          await(emailQueue.resetProcessing)

          val resetCount: Long = await(emailQueue.countDocuments(false))
          resetCount must be(3)
        }
      }
  }


  trait Setup{
    val mockAppConfig: AppConfig = mock[AppConfig]
    val app = new GuiceApplicationBuilder().build()
    val emailQueue = app.injector.instanceOf[EmailQueue]
    await(emailQueue.removeAll)
  }

}
