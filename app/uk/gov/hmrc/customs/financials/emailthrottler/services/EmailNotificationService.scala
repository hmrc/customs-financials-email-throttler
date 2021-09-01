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

import play.api.http.Status
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.EmailRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailNotificationService @Inject()(http: HttpClient, metricsReporter: MetricsReporterService)
                                        (implicit appConfig: AppConfig, ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def sendEmail(request: EmailRequest): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    metricsReporter.withResponseTimeLogging("email.post.send-email") {
      http.POST[EmailRequest, HttpResponse](appConfig.sendEmailUrl, request).collect {
        case response if response.status == Status.ACCEPTED =>
          log.info(s"[SendEmail] Successful for ${request.to}")
          true
        case response =>
          log.error(s"[SendEmail] Failed for ${request.to} with status - ${response.status} error - ${response.body}")
          false
      }.recover {
        case ex: Throwable =>
          log.error(s"[SendEmail] Received an exception with message - ${ex.getMessage}")
          false
      }
    }
  }
}
