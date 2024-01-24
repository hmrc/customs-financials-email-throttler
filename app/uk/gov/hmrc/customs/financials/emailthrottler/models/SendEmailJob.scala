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

package uk.gov.hmrc.customs.financials.emailthrottler.models

import play.api.libs.json._
import java.time.{Instant, ZoneOffset}
import java.time.LocalDateTime

case class SendEmailJob(_id: String, emailRequest: EmailRequest, processing: Boolean, lastUpdated: LocalDateTime)

trait MongoJavatimeFormats {
  outer =>

  final val localDateTimeReads: Reads[LocalDateTime] =
    Reads.at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  final val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  final val localDateTimeFormat: Format[LocalDateTime] =
    Format(localDateTimeReads, localDateTimeWrites)

  trait Implicits {
    implicit val jatLocalDateTimeFormat: Format[LocalDateTime] = outer.localDateTimeFormat
  }

  object Implicits extends Implicits
}

object MongoJavatimeFormats extends MongoJavatimeFormats

object SendEmailJob {
  implicit val mongoDateTime: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val formatSendEmailJob: OFormat[SendEmailJob] = Json.format[SendEmailJob]
}
