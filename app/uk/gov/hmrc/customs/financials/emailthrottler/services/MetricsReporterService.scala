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

import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import play.api.http.Status
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MetricsReporterService @Inject()(val metrics: Metrics, dateTimeService: DateTimeService) {

  def withResponseTimeLogging[T](resourceName: String)(future: Future[T])
                                (implicit ec: ExecutionContext): Future[T] = {
    val startTime = dateTimeService.getLocalDateTime
    future.andThen { case response =>
      val httpResponseCode = response match {
        case Success(_) => Status.OK
        case Failure(exception: NotFoundException) => exception.responseCode
        case Failure(exception: BadRequestException) => exception.responseCode
        case Failure(exception: UpstreamErrorResponse) => exception.statusCode
        case Failure(_) => Status.INTERNAL_SERVER_ERROR
      }
      updateResponseTimeHistogram(resourceName, httpResponseCode, startTime, dateTimeService.getLocalDateTime)
    }
  }

  private def updateResponseTimeHistogram(resourceName: String, httpResponseCode: Int,
                                          startTimestamp: LocalDateTime, endTimestamp: LocalDateTime): Unit = {
    val RESPONSE_TIMES_METRIC = "responseTimes"
    val histogramName = s"$RESPONSE_TIMES_METRIC.$resourceName.$httpResponseCode"
    val elapsedTimeInMillis = endTimestamp.toInstant(ZoneOffset.UTC).toEpochMilli - startTimestamp.toInstant(ZoneOffset.UTC).toEpochMilli
    metrics.defaultRegistry.histogram(histogramName).update(elapsedTimeInMillis)
  }

  val EMAIL_QUEUE_METRIC = "email-queue"

  def reportSuccessfulEnqueueJob(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.enqueue-send-email-job-in-mongo-successful"
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def reportFailedEnqueueJob(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.enqueue-send-email-job-in-mongo-failed"
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def reportSuccessfulMarkJobForProcessing(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.mark-oldest-send-email-job-for-processing-in-mongo-successful"
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def reportFailedMarkJobForProcessing(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.mark-oldest-send-email-job-for-processing-in-mongo-failed"
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def reportSuccessfullyRemoveCompletedJob(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.delete-completed-send-email-job-from-mongo-successful"
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def reportFailedToRemoveCompletedJob(): Unit = {
    val counterName = s"$EMAIL_QUEUE_METRIC.delete-completed-send-email-job-from-mongo-failed"
    metrics.defaultRegistry.counter(counterName).inc()
  }

}
