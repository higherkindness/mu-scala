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
import org.scalacheck.Gen.Choose
import org.scalacheck.{Arbitrary, Gen, Prop, Test}
import org.scalacheck.Prop._

class BigDecimalUtilTests extends ScalaCheckSuite {

  override protected def scalaCheckTestParameters: Test.Parameters =
    Test.Parameters.default.withMinSuccessfulTests(1000)

  def checkAll[T](f: T => BigDecimal)(implicit N: Numeric[T], C: Choose[T]): Prop = {
    implicit val bigDecimalArbitrary: Arbitrary[BigDecimal] =
      Arbitrary(Gen.posNum[T].map(f))

    forAll { bd: BigDecimal =>
      val array = BigDecimalUtil.bigDecimalToByte(bd)
      BigDecimalUtil.byteToBigDecimal(array) == bd
    }
  }

  property("BigDecimalUtil should allow to convert BigDecimals to and from byte arrays") {
    forAll { bd: BigDecimal =>
      val array = BigDecimalUtil.bigDecimalToByte(bd)
      BigDecimalUtil.byteToBigDecimal(array) == bd
    }
  }

  property(
    "BigDecimalUtil should allow to convert BigDecimals created from doubles to and from byte arrays"
  ) {
    checkAll[Double](BigDecimal.decimal)
  }

  property(
    "BigDecimalUtil should allow to convert BigDecimals created from floats to and from byte arrays"
  ) {
    checkAll[Float](BigDecimal.decimal)
  }

  property(
    "BigDecimalUtil should allow to convert BigDecimals created from longs to and from byte arrays"
  ) {
    checkAll[Long](BigDecimal.decimal)
  }

}
