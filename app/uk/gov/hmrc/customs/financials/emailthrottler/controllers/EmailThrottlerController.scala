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

package uk.gov.hmrc.customs.financials.emailthrottler.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.emailthrottler.models.EmailRequest
import uk.gov.hmrc.customs.financials.emailthrottler.services.EmailQueue
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EmailThrottlerController @Inject() (emailQueue: EmailQueue, cc: ControllerComponents)(implicit
  ec: ExecutionContext
) extends BackendController(cc) {

  val log: LoggerLike = Logger(this.getClass)

  def enqueueEmail(): Action[EmailRequest] = Action.async(parse.json[EmailRequest]) { implicit request =>
    log.info(s"enqueueEmail: send email request enqueued")
    emailQueue.enqueueJob(request.body.formattedEnrolment)
    Future.successful(Accepted(Json.obj("Status" -> "Ok", "message" -> "Email successfully queued")))
  }
}
