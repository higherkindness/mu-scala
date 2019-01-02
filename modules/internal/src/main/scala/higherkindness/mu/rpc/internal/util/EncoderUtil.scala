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

import java.nio.ByteBuffer

object EncoderUtil {

  def intToByteArray(value: Int): Array[Byte] = {
    val byteBuffer = ByteBuffer.allocate(4)
    byteBuffer.putInt(value)
    byteBuffer.array()
  }

  def byteArrayToInt(value: Array[Byte]): Int =
    ByteBuffer.wrap(value).getInt

  def longToByteArray(value: Long): Array[Byte] = {
    val byteBuffer = ByteBuffer.allocate(8)
    byteBuffer.putLong(value)
    byteBuffer.array()
  }

  def byteArrayToLong(value: Array[Byte]): Long =
    ByteBuffer.wrap(value).getLong

}
