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

import play.api.libs.json.*
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.customs.financials.emailthrottler.utils.JsonFormatUtils

case class EmailAddress(address: String) {
  override def toString: String = "*" * address.length
}

case class EmailRequest(
  to: List[EmailAddress],
  templateId: String,
  parameters: Map[String, String] = Map.empty,
  force: Boolean = false,
  enrolment: Option[String] = None,
  eventUrl: Option[String] = None,
  onSendUrl: Option[String] = None
) {

  def formattedEnrolment: EmailRequest = {
    val formattedEnrolment = enrolment.map(e => s"HMRC-CUS-ORG~EORINumber~$e")
    EmailRequest(to, templateId, parameters, force, formattedEnrolment, eventUrl, onSendUrl)
  }

}

object EmailRequest {
  implicit val emailAddressFormat: Format[EmailAddress]  = JsonFormatUtils.stringFormat(EmailAddress.apply)(_.address)
  implicit val emailRequestFormat: OFormat[EmailRequest] = Json.format[EmailRequest]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
