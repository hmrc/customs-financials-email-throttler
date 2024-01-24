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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class EmailJobHandler @Inject()(emailQueue: EmailQueue,
                                emailNotificationService: EmailNotificationService)(implicit ec: ExecutionContext) {

  def processJob(): Future[Unit] = {
    for {
      job <- emailQueue.nextJob if job.isDefined
      emailRequest = job.get.emailRequest
      _ <- emailNotificationService.sendEmail(emailRequest)
      id = job.get._id
      _ <- emailQueue.deleteJob(id)
    } yield ()
  }

  def houseKeeping(): Unit = emailQueue.resetProcessing
}
