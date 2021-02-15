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

package uk.gov.hmrc.customs.financials.emailthrottler.config

import org.scalatest.{Matchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Environment}

class AppConfigSpec extends WordSpec with Matchers {

  val environment: Environment = Environment.simple()
  val configuration: Configuration = Configuration.load(environment)
  val application = new GuiceApplicationBuilder().build()
  val appConfig = application.injector.instanceOf[AppConfig]

  "AppConfig" should {

    "have settings for graphite" in {
      appConfig.graphiteHost shouldBe "graphite"
    }

    "have settings for sendEmailUrl" in {
      appConfig.sendEmailUrl endsWith "/hmrc/email"
    }

    "have emailsPerInstancePerSecond configured" in {
      appConfig.emailsPerInstancePerSecond shouldBe 5
    }

    "have defaultEmailsPerInstancePerSecond configured" in {
      appConfig.defaultEmailsPerInstancePerSecond shouldBe 0.1
    }
  }
}
