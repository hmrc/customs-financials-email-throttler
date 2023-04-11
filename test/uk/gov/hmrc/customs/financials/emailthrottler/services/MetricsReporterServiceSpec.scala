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

import com.codahale.metrics.{Counter, Histogram, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, verify, when}
import play.api.http.Status
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase
import uk.gov.hmrc.http._

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetricsReporterServiceSpec extends SpecBase {
  "MetricsReporterService" should {

    "Email Queue metrics" should {
      "reportSuccessfulEnqueueJob" in new Setup {
        metricsReporterService.reportSuccessfulEnqueueJob()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.enqueue-send-email-job-in-mongo-successful"))
        verify(mockCounter).inc()
      }
      "reportFailedEnqueueJob" in new Setup {
        metricsReporterService.reportFailedEnqueueJob()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.enqueue-send-email-job-in-mongo-failed"))
        verify(mockCounter).inc()
      }
      "reportSuccessfulMarkJobForProcessing" in new Setup {
        metricsReporterService.reportSuccessfulMarkJobForProcessing()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.mark-oldest-send-email-job-for-processing-in-mongo-successful"))
        verify(mockCounter).inc()
      }
      "reportFailedMarkJobForProcessing" in new Setup {
        metricsReporterService.reportFailedMarkJobForProcessing()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.mark-oldest-send-email-job-for-processing-in-mongo-failed"))
        verify(mockCounter).inc()
      }
      "reportSuccessfulRemoveCompletedJob" in new Setup {
        metricsReporterService.reportSuccessfullyRemoveCompletedJob()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.delete-completed-send-email-job-from-mongo-successful"))
        verify(mockCounter).inc()
      }
      "reportFailedRemoveCompletedJob" in new Setup {
        metricsReporterService.reportFailedToRemoveCompletedJob()
        verify(mockRegistry).counter(ArgumentMatchers.eq("email-queue.delete-completed-send-email-job-from-mongo-failed"))
        verify(mockCounter).inc()
      }
    }

    "withResponseTimeLogging" should {

      "log successful call metrics" in new Setup {
        await {
          metricsReporterService.withResponseTimeLogging("foo") {
            Future.successful("OK")
          }
        }
        verify(mockRegistry).histogram("responseTimes.foo.200")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

      "log default error during call metrics" in new Setup {
        assertThrows[InternalServerException] {
          await {
            metricsReporterService.withResponseTimeLogging("bar") {
              Future.failed(new InternalServerException("boom"))
            }
          }
        }
        verify(mockRegistry).histogram("responseTimes.bar.500")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

      "log not found call metrics" in new Setup {
        assertThrows[NotFoundException] {
          await {
            metricsReporterService.withResponseTimeLogging("bar") {
              Future.failed(new NotFoundException("boom"))
            }
          }
        }
        verify(mockRegistry).histogram("responseTimes.bar.404")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

      "log bad request error call metrics" in new Setup {
        assertThrows[BadRequestException] {
          await {
            metricsReporterService.withResponseTimeLogging("bar") {
              Future.failed(new BadRequestException("boom"))
            }
          }
        }
        verify(mockRegistry).histogram("responseTimes.bar.400")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

      "log 5xx error call metrics" in new Setup {
        assertThrows[UpstreamErrorResponse] {
          await {
            metricsReporterService.withResponseTimeLogging("bar") {
              Future.failed(UpstreamErrorResponse("failure", Status.SERVICE_UNAVAILABLE, Status.NOT_IMPLEMENTED))
            }
          }
        }
        verify(mockRegistry).histogram("responseTimes.bar.503")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

      "log 4xx error call metrics" in new Setup {
        assertThrows[UpstreamErrorResponse] {
          await {
            metricsReporterService.withResponseTimeLogging("bar") {
              Future.failed(UpstreamErrorResponse("failure", Status.FORBIDDEN, Status.NOT_IMPLEMENTED))
            }
          }
        }
        verify(mockRegistry).histogram("responseTimes.bar.403")
        verify(mockHistogram).update(elapsedTimeInMillis)
      }

    }

  }

  trait Setup {
    val mockDateTimeService: DateTimeService = mock(classOf[DateTimeService])
    val startTimestamp: LocalDateTime = LocalDateTime.of(2018, 11, 9, 17, 15, 30, 1)
    val endTimestamp: LocalDateTime = LocalDateTime.of(2018, 11, 9, 17, 15, 35, 1)
    val elapsedTimeInMillis: Long = 5000L // endTimestamp - startTimestamp
    when(mockDateTimeService.getLocalDateTime)
      .thenReturn(startTimestamp)
      .thenReturn(endTimestamp)

    val mockHistogram: Histogram = mock(classOf[Histogram])
    val mockCounter: Counter = mock(classOf[Counter])
    when(mockCounter.inc()).thenCallRealMethod()

    val mockRegistry: MetricRegistry = mock(classOf[MetricRegistry])
    when(mockRegistry.histogram(ArgumentMatchers.any())).thenReturn(mockHistogram)
    when(mockRegistry.counter(ArgumentMatchers.any())).thenReturn(mockCounter)

    val mockMetrics: Metrics = mock(classOf[Metrics])
    when(mockMetrics.defaultRegistry).thenReturn(mockRegistry)

    val metricsReporterService = new MetricsReporterService(mockMetrics, mockDateTimeService)
  }
}
