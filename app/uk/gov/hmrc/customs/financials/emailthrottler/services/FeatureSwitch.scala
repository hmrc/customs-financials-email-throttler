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

import com.typesafe.config.ConfigFactory

object FeatureSwitch {

  private val configuration = ConfigFactory.load()

  def forName(name: String): FeatureName = { // scalastyle:ignore cyclomatic.complexity
    name match {
      case EmailNotifications.name => EmailNotifications

      case ActualSps.name => ActualSps
    }
  }

  sealed trait FeatureName {

    val name: String

    val confPropertyName: String = s"features.$name"

    val systemPropertyName: String = s"features.$name"

    def isEnabled(): Boolean = {
      val sysPropValue = sys.props.get(systemPropertyName)
      sysPropValue match {
        case Some(x) =>
          x.toBoolean
        case None =>
          if (configuration.hasPath(confPropertyName)) ConfigFactory.load().getBoolean(confPropertyName) else false
      }
    }

    def enable() {
      setProp(true)
    }

    def disable() {
      setProp(false)
    }

    def setProp(value: Boolean) {
      val systemProps = sys.props.+=((systemPropertyName, value.toString))
    }
  }

  case object EmailNotifications extends {val name = "email-notifications"} with FeatureName

  case object ActualSps extends {val name = "actual-sps"} with FeatureName

}
