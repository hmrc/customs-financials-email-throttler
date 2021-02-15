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

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Logger, LoggerLike, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

//noinspection TypeAnnotation
@Singleton
class AppConfig @Inject()(val config: Configuration, servicesConfig: ServicesConfig, environment: Environment) {

  val log: LoggerLike = Logger(this.getClass)

  lazy val appName = servicesConfig.getString("appName")

  protected def mode: Mode = environment.mode

  val graphiteHost: String = servicesConfig.getString("microservice.metrics.graphite.host")

  lazy val sendEmailUrl = servicesConfig.baseUrl("email") + "/hmrc/email"

  val defaultEmailsPerInstancePerSecond = 0.1
  val emailsPerInstancePerSecond = config.getOptional[Double]("emailsPerInstancePerSecond").getOrElse(defaultEmailsPerInstancePerSecond)

  log.info(s"emails per instance per second: $emailsPerInstancePerSecond")

}
