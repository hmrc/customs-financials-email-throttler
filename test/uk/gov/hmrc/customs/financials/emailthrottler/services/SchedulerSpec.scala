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

import akka.actor.ActorSystem
import org.mockito.Mockito.{times, verify, when, mock}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global

class SchedulerSpec extends SpecBase {

  "Scheduler" should {
    "schedule email sending" in {
      val mockAppConfig = mock(classOf[AppConfig])
      when(mockAppConfig.emailsPerInstancePerSecond).thenReturn(0.2)
      val mockEmailJobHandler = mock(classOf[EmailJobHandler])
      val mockActorSystem = mock(classOf[ActorSystem])
      val mockScheduler = mock(classOf[akka.actor.Scheduler])
      when(mockActorSystem.scheduler).thenReturn(mockScheduler)
      new Scheduler(mockAppConfig, mockEmailJobHandler, mockActorSystem)
      verify(mockActorSystem, times(2)).scheduler
    }
  }
}
