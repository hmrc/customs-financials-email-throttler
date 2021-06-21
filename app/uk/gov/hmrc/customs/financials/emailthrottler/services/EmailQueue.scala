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

import com.mongodb.client.model.Indexes.{ascending, descending}
import com.mongodb.client.model.{ReplaceOptions, Updates}
import org.mongodb.scala.FindObservable
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, ReplaceOptions}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailRequest, SendEmailJob}
import uk.gov.hmrc.mongo.play.PlayMongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.ZoneOffset
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class EmailQueue @Inject()(mongoComponent: PlayMongoComponent,
                           dateTimeService: DateTimeService,
                           appConfig: AppConfig,
                           metricsReporter: MetricsReporterService)
                          (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SendEmailJob](
    collectionName = "email-queue",
    mongoComponent = mongoComponent,
    domainFormat = SendEmailJob.formatSendEmailJob,
    indexes = Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions().name("email-queue-last-updated-index")
          .background(false)
      )
    )) {

  val logger: LoggerLike = Logger(this.getClass)

  def enqueueJob(emailRequest: EmailRequest): Future[Boolean] = {
    val timeStamp = dateTimeService.getLocalDateTime
    val id = UUID.randomUUID().toString
    val record = SendEmailJob(id, emailRequest, processing = false, timeStamp)
    val result: Future[Boolean] = collection.insertOne(record).toFuture().map(_.wasAcknowledged())
    result.onComplete {
      case Failure(error) =>
        metricsReporter.reportFailedEnqueueJob()
        logger.error(s"Could not enqueue send email job: ${error.getMessage}")
      case Success(_) =>
        metricsReporter.reportSuccessfulEnqueueJob()
        logger.info(s"Successfully enqueued send email job:  $timeStamp : $emailRequest")
    }
    result
  }

  def nextJob: Future[Option[SendEmailJob]] = {
    collection.findOneAndUpdate(
      equal("processing", false),
      Updates.set("processing", true)
    ).toFutureOption().map {
      case emailJob@Some(value) =>
        metricsReporter.reportSuccessfulMarkJobForProcessing()
        logger.info(s"Successfully marked latest send email job for processing: ${value}")
        emailJob
      case None =>
        logger.debug(s"email queue is empty")
        None
    }.recover {
      case m => metricsReporter.reportFailedMarkJobForProcessing()
        logger.error(s"Marking send email job for processing failed. Unexpected MongoDB error: $m")
        throw m
    }
  }

  def deleteJob(id: String): Future[Boolean] = {
    val result = collection.deleteOne(equal("_id", id)).toFuture().map(_.wasAcknowledged())
    result.onComplete {
      case Success(_) =>
        metricsReporter.reportSuccessfullyRemoveCompletedJob()
        logger.info(s"Successfully deleted send email job: $id")
      case Failure(error) =>
        metricsReporter.reportFailedToRemoveCompletedJob()
        logger.error(s"Could not delete completed send email job: $error")
    }
    result
  }

  def resetProcessing: Future[Unit] = {
    val maxAge = dateTimeService.getLocalDateTime.minusMinutes(appConfig.emailMaxAgeMins)
    val updates = Updates.set("processing", false)
    collection.updateMany(
      filter = Filters.and(
        Filters.equal("processing", true),
        Filters.lt("lastUpdated", maxAge.toInstant(ZoneOffset.UTC))
      ),
      updates
    ).toFuture().map(_ => ())
  }
}
