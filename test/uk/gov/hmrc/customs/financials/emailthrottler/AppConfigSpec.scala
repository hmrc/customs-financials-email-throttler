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

package uk.gov.hmrc.customs.financials.emailthrottler

import org.mockito.Mockito.mock
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.customs.financials.emailthrottler.services.Scheduler
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, inject}
import config.AppConfig

class AppConfigSpec extends SpecBase {

  "AppConfig" should {

    "return correct output for appName" in new Setup {
      appConfig.appName mustBe "customs-financials-email-throttler"
    }

    "return correct output for sendEmailUrl" in new Setup {
      appConfig.sendEmailUrl mustBe "http://localhost:8300/hmrc/email"
    }

    "return correct output for emailsPerInstancePerSecond" in new Setup {
      appConfig.emailsPerInstancePerSecond mustBe 5
    }

    "return correct output for emailMaxAgeMins" in new Setup {
      appConfig.emailMaxAgeMins mustBe 30
    }

    "return correct output for housekeepingHours" in new Setup {
      appConfig.housekeepingHours mustBe 12
    }
  }

  trait Setup {
    val mockScheduler: Scheduler = mock(classOf[Scheduler])

    val app: Application = new GuiceApplicationBuilder()
      .configure("auditing.enabled" -> false)
      .configure("metrics.enabled" -> false)
      .overrides(inject.bind[Scheduler].toInstance(mockScheduler))
      .build()

    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  }
}
