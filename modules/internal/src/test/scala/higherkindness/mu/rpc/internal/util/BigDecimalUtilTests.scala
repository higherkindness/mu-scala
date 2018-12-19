/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

import org.scalacheck.Gen.Choose
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class BigDecimalUtilTests extends WordSpec with Matchers with Checkers {

  def checkAll[T](f: T => BigDecimal)(implicit N: Numeric[T], C: Choose[T]): Assertion = {
    implicit val bigDecimalArbitrary: Arbitrary[BigDecimal] =
      Arbitrary(Gen.posNum[T].map(f))

    check(
      forAll { bd: BigDecimal =>
        val array = BigDecimalUtil.bigDecimalToByte(bd)
        BigDecimalUtil.byteToBigDecimal(array) == bd
      },
      MinSuccessful(1000)
    )
  }

  "BigDecimalUtil" should {

    "allow to convert BigDecimals to and from byte arrays" in {
      check(
        forAll { bd: BigDecimal =>
          val array = BigDecimalUtil.bigDecimalToByte(bd)
          BigDecimalUtil.byteToBigDecimal(array) == bd
        },
        MinSuccessful(1000)
      )
    }

    "allow to convert BigDecimals created from doubles to and from byte arrays" in {
      checkAll[Double](BigDecimal.decimal)
    }

    "allow to convert BigDecimals created from floats to and from byte arrays" in {
      checkAll[Float](BigDecimal.decimal)
    }

    "allow to convert BigDecimals created from longs to and from byte arrays" in {
      checkAll[Long](BigDecimal.decimal)
    }
  }

}
