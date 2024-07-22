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

import play.api.http.Status
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.EmailRequest
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailNotificationService @Inject()(http: HttpClientV2, metricsReporter: MetricsReporterService)
                                        (implicit appConfig: AppConfig, ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def sendEmail(request: EmailRequest): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging("email.post.send-email") {
      http.post(url"${appConfig.sendEmailUrl}")
        .withBody[EmailRequest](request)
        .execute[HttpResponse]
        .flatMap {
          case response if response.status == Status.ACCEPTED => Future.successful(log.info(
            s"[SendEmail] Successful for ${request.to}"))
          Future.successful(true)
          case response => Future.successful(log.error(
            s"[SendEmail] Failed for ${
              request.to} with status - ${response.status} error - ${response.body}"))
            Future.successful(false)
        }.recover {
          case ex: Throwable => log.error(
            s"[SendEmail] Received an exception with message - ${ex.getMessage}")
          false
        }
    }
  }
}
