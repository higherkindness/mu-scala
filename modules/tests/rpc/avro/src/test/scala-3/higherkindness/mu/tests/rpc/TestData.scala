/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.tests.rpc

import higherkindness.mu.rpc.avro.AvroUnion4
import higherkindness.mu.rpc.avro.Decimals.TaggedDecimal

object TestData {

  val request = Request(
    a = 123,
    b = TaggedDecimal[8, 4](BigDecimal("3.456")).getOrElse(throw new RuntimeException("nope!")),
    c = AvroUnion4[Long, String, Boolean, Foo](Foo("wow")),
    d = Direction.SOUTH
  )

}
