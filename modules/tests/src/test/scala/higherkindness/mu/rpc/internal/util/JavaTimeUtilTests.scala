/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

import java.time.{Duration, ZoneOffset, ZonedDateTime}
import com.fortysevendeg.scalacheck.datetime.instances.jdk8._
import com.fortysevendeg.scalacheck.datetime.GenDateTime._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class JavaTimeUtilTests extends ScalaCheckSuite {

  val from: ZonedDateTime = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val range: Duration     = Duration.ofDays(365 * 200)

  property("JavaTimeUtil should allow to convert LocalDate to and from int") {
    import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.days
    forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
      val date  = zdt.toLocalDate
      val value = JavaTimeUtil.localDateToInt(date)
      JavaTimeUtil.intToLocalDate(value) == date
    }
  }

  property("JavaTimeUtil should allow to convert LocalDateTime to and from long") {
    import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds
    forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
      val date  = zdt.toLocalDateTime
      val value = JavaTimeUtil.localDateTimeToLong(date)
      JavaTimeUtil.longToLocalDateTime(value) == date
    }
  }

  property("JavaTimeUtil should allow to convert Instant to and from long") {
    import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds
    forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
      val date  = zdt.toInstant
      val value = JavaTimeUtil.instantToLong(date)
      JavaTimeUtil.longToInstant(value) == date
    }
  }
}
