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

object BigDecimalUtil {

  def bigDecimalToByte(num: BigDecimal): Array[Byte] = {
    val sig: Array[Byte] = num.bigDecimal.unscaledValue().toByteArray
    val scale: Int       = num.scale
    val byteScale: Array[Byte] =
      Array[Byte]((scale >>> 24).toByte, (scale >>> 16).toByte, (scale >>> 8).toByte, scale.toByte)
    byteScale ++ sig
  }

  def byteToBigDecimal(raw: Array[Byte]): BigDecimal = {
    val scale = (raw(0) & 0xFF) << 24 | (raw(1) & 0xFF) << 16 | (raw(2) & 0xFF) << 8 | (raw(3) & 0xFF)
    val sig   = new java.math.BigInteger(raw.drop(4))
    BigDecimal(sig, scale)
  }

}
