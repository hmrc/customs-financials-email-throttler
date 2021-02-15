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

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class Scheduler @Inject()(appConfig: AppConfig, emailJobHandler: EmailJobHandler, actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {

  val emailsPerInstancePerSecond = appConfig.emailsPerInstancePerSecond

  actorSystem.scheduler.schedule(initialDelay = 0 seconds, interval = 1/emailsPerInstancePerSecond second) {
    emailJobHandler.processJob()
  }
}
