/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc
package internal
package util

import org.joda.time._
import com.fortysevendeg.scalacheck.datetime.instances.joda._
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import higherkindness.mu.rpc.jodatime.util.JodaTimeUtil
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class JodaTimeUtilTest extends WordSpec with Matchers with Checkers {

  val from: DateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)

  val range: Period = Period.years(200)

  "JodaTimeUtil" should {

    "allow to convert LocalDate to and from int" in {
      import com.fortysevendeg.scalacheck.datetime.joda.granularity.days
      check {
        forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
          val date: LocalDate = dt.toLocalDate
          val value: Int      = JodaTimeUtil.jodaLocalDateToInt(date)

          JodaTimeUtil.intToJodaLocalDate(value) == date
        }
      }
    }

    "allow to convert LocalDateTime to and from long" in {
      import com.fortysevendeg.scalacheck.datetime.joda.granularity.seconds
      check {
        forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
          val date: LocalDateTime = dt.toDateTime(DateTimeZone.UTC).toLocalDateTime
          val value: Long         = JodaTimeUtil.jodaLocalDatetimeToLong(date)

          JodaTimeUtil.longToJodaLocalDateTime(value) == date
        }
      }
    }
  }

}
