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

import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.Updates
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, UpdateOptions}
import play.api.libs.json.Json
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.models.{EmailRequest, SendEmailJob}
import uk.gov.hmrc.mongo.play.PlayMongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class EmailQueue @Inject()(mongoComponent: PlayMongoComponent,
                           appConfig: AppConfig,
                           dateTimeService: DateTimeService,
                           metricsReporter: MetricsReporterService)
                          (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SendEmailJob](
    collectionName = "emailQueue",
    mongoComponent = mongoComponent,
    domainFormat = SendEmailJob.formatSendEmailJob,
    indexes = Seq(
      IndexModel(
        ascending("timeStampAndCRL"),
        IndexOptions().name("timestampIndex")
          .unique(false)
          .background(true)
          .sparse(false)
      )
    )) {

  val logger: LoggerLike = Logger(this.getClass)

  def enqueueJob(emailRequest: EmailRequest): Future[Boolean] = {
    val timeStamp = dateTimeService.getTimeStamp
    val id = UUID.randomUUID().toString
    val record = SendEmailJob(id, emailRequest, timeStamp, processing = false)
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
    val eventualResult: Future[Option[SendEmailJob]] = collection.findOneAndUpdate(
      filter = equal("processing", false),
      update = Updates.set("processing", true)).toFutureOption()


    eventualResult.onComplete {
      case Success(Some(result))  =>
        metricsReporter.reportSuccessfulMarkJobForProcessing()
        logger.info(s"Successfully marked latest send email job for processing: ${result}")
      case Success(None)  =>
        logger.debug(s"email queue is empty")
      case m =>
        metricsReporter.reportFailedMarkJobForProcessing()
        logger.error(s"Marking send email job for processing failed. Unexpected MongoDB error: $m")
    }

    eventualResult
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
    val maxAge = dateTimeService.getTimeStamp.minusMinutes(appConfig.emailMaxAgeMins)
    val updates = Updates.set("processing", false)
    collection.updateMany(
      filter =  Filters.and(
        Filters.equal("processing", true),
        Filters.lt("timeStampAndCRL", Codecs.toBson(Json.toJson(maxAge)))
      ),
      updates,
      UpdateOptions().upsert(true)
    ).toFuture().map(_ => ())
  }

  def removeAll:Future[Unit] = {
    collection.drop().toFuture().map(_ => ())
  }

  def countDocuments(processing: Boolean) : Future[Long] = {
    collection.countDocuments(
      filter = Filters.equal("processing", processing)).toFuture().map(s => s)
  }
}
