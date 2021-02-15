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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.financials.emailthrottler.config.AppConfig
import uk.gov.hmrc.customs.financials.emailthrottler.domain.{EmailRequest, SendEmailJob}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class EmailQueue @Inject()(mongoComponent: ReactiveMongoComponent,
                           appConfig: AppConfig,
                           dateTimeService: DateTimeService,
                           metricsReporter: MetricsReporterService)
                          (implicit ec: ExecutionContext)
  extends ReactiveRepository[SendEmailJob, BSONObjectID](
    collectionName = "emailQueue",
    mongo = mongoComponent.mongoConnector.db,
    domainFormat = SendEmailJob.formatSendEmailJob,
    idFormat = ReactiveMongoFormats.objectIdFormats) {

  private val timestampIndex = Index(
    Seq("timeStampAndCRL" -> IndexType.Ascending),
    name = Some("timestampIndex"),
    unique = false,
    background = true,
    sparse = false)

  // TODO: mongo does not recreate existing indexes, we have to drop it once, remove after deployed
  //collection.indexesManager.ensure(timestampIndex)
  collection.indexesManager.drop("timestampIndex").andThen { case dropResult =>
    logger.info(s"drop timestampIndex result: $dropResult")
    collection.indexesManager.ensure(timestampIndex).andThen { case createIndexResult =>
      logger.info(s"create timestampIndex result: $createIndexResult")
    }
  }

  def enqueueJob(emailRequest: EmailRequest): Future[Unit] = {
    val timeStamp = dateTimeService.getTimeStamp

    val result: Future[WriteResult] = insert(SendEmailJob(BSONObjectID.generate, emailRequest, timeStamp, processing = false))
    result.onComplete {
      case Failure(error) =>
        metricsReporter.reportFailedEnqueueJob()
        logger.error(s"Could not enqueue send email job: ${error.getMessage}")
      case Success(_) =>
        metricsReporter.reportSuccessfulEnqueueJob()
        logger.info(s"Successfully enqueued send email job:  $timeStamp : $emailRequest")
    }

    result.map(_=>())
  }

  def nextJob: Future[Option[SendEmailJob]] = {
    val eventualResult = findAndUpdate(
        query = Json.obj("processing" -> Json.toJsFieldJsValueWrapper(false)),
        update = Json.obj("$set" -> Json.obj("processing" -> Json.toJsFieldJsValueWrapper(true))),
        sort = Some(Json.obj("timeStampAndCRL" -> Json.toJsFieldJsValueWrapper(1))),
        fetchNewObject = true
    )

    eventualResult.onComplete {
      case Success(result) if(result.value.isDefined) =>
          metricsReporter.reportSuccessfulMarkJobForProcessing()
          logger.info(s"Successfully marked latest send email job for processing: ${result.result[SendEmailJob]}")
      case Success(result) if(result.lastError.isDefined && result.lastError.get.err.isEmpty) =>
          logger.debug(s"email queue is empty")
      case m =>
        metricsReporter.reportFailedMarkJobForProcessing()
        logger.error(s"Marking send email job for processing failed. Unexpected MongoDB error: $m")
    }

    eventualResult.map(_.result[SendEmailJob])
  }

  def deleteJob(id: BSONObjectID): Future[Unit] = {
    val result = removeById(id)
    result.onComplete {
      case Success(_) =>
        metricsReporter.reportSuccessfullyRemoveCompletedJob()
        logger.info(s"Successfully deleted send email job: $id")
      case Failure(error) =>
        metricsReporter.reportFailedToRemoveCompletedJob()
        logger.error(s"Could not delete completed send email job: $error")
    }

    result.map(_=>())
  }

}
