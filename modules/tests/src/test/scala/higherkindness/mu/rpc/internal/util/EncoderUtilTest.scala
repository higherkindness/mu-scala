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

import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class EncoderUtilTest extends ScalaCheckSuite {

  property("EncoderUtil should allow to convert int to and from byteArray") {
    forAll { n: Int =>
      val value: Array[Byte] = EncoderUtil.intToByteArray(n)

      EncoderUtil.byteArrayToInt(value) == n

    }
  }

  property("EncoderUtil should allow to convert long to and from byteArray") {
    forAll { n: Long =>
      val value: Array[Byte] = EncoderUtil.longToByteArray(n)

      EncoderUtil.byteArrayToLong(value) == n

    }
  }

}
