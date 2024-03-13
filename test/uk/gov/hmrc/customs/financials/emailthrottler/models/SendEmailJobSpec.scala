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

import org.mockito.Mockito.{mock, when}
import play.api.libs.json._
import uk.gov.hmrc.customs.financials.emailthrottler.utils.SpecBase

import java.time.{Instant, LocalDateTime, ZoneOffset}

class SendEmailJobSpec extends SpecBase {

  "MongoJavatimeFormats" when {
    "writes" in new Setup {
      val res = mockMongoFormats.localDateTimeWrites
      res mustBe testWrites
    }

    "reads" in new Setup {
      val res = mockMongoFormats.localDateTimeReads
      res mustBe testReads
    }
  }

  "SendEmailJob" when {
    "mongoDateTime returns valid date" in new Setup {
      val res = SendEmailJob.mongoDateTime
      res mustBe testDateTime
    }
  }

  trait Setup {

    final val testReads: Reads[LocalDateTime] = Reads.at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

    final val testWrites: Writes[LocalDateTime] = Writes.at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

    implicit val testDateTime: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat

    val mockMongoFormats: MongoJavatimeFormats = mock(classOf[MongoJavatimeFormats])

    when(mockMongoFormats.localDateTimeReads).thenReturn(testReads)
    when(mockMongoFormats.localDateTimeWrites).thenReturn(testWrites)
  }
}
