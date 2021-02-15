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

import org.scalatest.{AppendedClues, BeforeAndAfterEach, MustMatchers, WordSpec}

class FeatureSwitchSpec extends WordSpec with BeforeAndAfterEach with MustMatchers with AppendedClues {

  override def beforeEach: Unit = {
    System.clearProperty("features.email-notifications")
  }

  "FeatureSwitch" should {

    "generate correct system property name for the feature" in {
      FeatureSwitch.EmailNotifications.systemPropertyName must be("features.email-notifications")
    }

    "generate system property value as false by default" in {
      FeatureSwitch.forName("email-notifications").isEnabled() must be(true) withClue ", feature email-notifications is turned off"
    }

    "be ENABLED if the system property is defined as 'true'" in {
      System.setProperty("features.email-notifications", "true")

      FeatureSwitch.forName("email-notifications").isEnabled() must be(true)
    }

    "be DISABLED if the system property is defined as 'false'" in {
      System.setProperty("features.email-notifications", "false")

      FeatureSwitch.forName("email-notifications").isEnabled() must be(false)
    }

    "support dynamic toggling" in {
      System.setProperty("features.email-notifications", "false")
      FeatureSwitch.EmailNotifications.enable()
      FeatureSwitch.forName("email-notifications").isEnabled() must be(true) withClue ", feature email-notifications is not turned on"
      FeatureSwitch.EmailNotifications.disable()
      FeatureSwitch.forName("email-notifications").isEnabled() must be(false) withClue ", feature email-notifications is not turned off"

    }

  }

}
