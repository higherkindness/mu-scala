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

package higherkindness.mu.rpc.avro

import Decimals.TaggedDecimal
import munit.FunSuite
import com.sksamuel.avro4s._
import org.apache.avro._
import scala.jdk.CollectionConverters._
import java.io._

class DecimalsSpec extends FunSuite {

  test("TaggedDecimal.apply - precision and scale equal to tags") {
    assertEquals(TaggedDecimal[6, 4](BigDecimal("12.3456")).isRight, true)
  }

  test("TaggedDecimal.apply - precision < tag") {
    assertEquals(TaggedDecimal[6, 4](BigDecimal("1.2345")).isRight, true)
  }

  test("TaggedDecimal.apply - precision > tag") {
    assertEquals(
      TaggedDecimal[6, 4](BigDecimal("123.4567")),
      Left(
        "Precision is too high. Maximum allowed precision is 6, but this BigDecimal has precision 7."
      )
    )
  }

  test("TaggedDecimal.apply - scale < tag (safe)") {
    val bd = BigDecimal("12.345")

    assertEquals(bd.scale, 3)
    assertEquals(bd.precision, 5)

    // Even if we increase the scale to 4 (the tag value),
    // it will not make the precision larger than 6,
    // so we can safely accept this BigDecimal
    assertEquals(bd.setScale(4).precision, 6)

    assertEquals(TaggedDecimal[6, 4](bd).isRight, true)
  }

  test("TaggedDecimal.apply - scale < tag (another safe case)") {
    val bd = BigDecimal("1.23")

    assertEquals(bd.scale, 2)
    assertEquals(bd.precision, 3)

    // Even if we increase the scale to 4 (the tag value),
    // it will not make the precision larger than 6,
    // so we can safely accept this BigDecimal
    assertEquals(bd.setScale(4).precision, 5)

    assertEquals(TaggedDecimal[6, 4](bd).isRight, true)
  }

  test("TaggedDecimal.apply - scale < tag (unsafe)") {
    val bd = BigDecimal("123.456")

    assertEquals(bd.scale, 3)
    assertEquals(bd.precision, 6)

    // If we increase the scale to 4 (the tag value),
    // it will make the precision larger than 6,
    // which would cause a runtime error when we try to build the Avro schema,
    // so we can NOT safely accept this BigDecimal
    assertEquals(bd.setScale(4).precision, 7)

    assertEquals(
      TaggedDecimal[6, 4](bd),
      Left(
        "Scale (3) is too low. Calling setScale(4) would cause precision to become 7, which exceeds the maximum precision (6)."
      )
    )
  }

  test("TaggedDecimal.apply - scale < tag (another unsafe case)") {
    val bd = BigDecimal("123.4")

    assertEquals(bd.scale, 1)
    assertEquals(bd.precision, 4)

    // If we increase the scale to 4 (the tag value),
    // it will make the precision larger than 6,
    // which would cause a runtime error when we try to build the Avro schema,
    // so we can NOT safely accept this BigDecimal
    assertEquals(bd.setScale(4).precision, 7)

    assertEquals(
      TaggedDecimal[6, 4](bd),
      Left(
        "Scale (1) is too low. Calling setScale(4) would cause precision to become 7, which exceeds the maximum precision (6)."
      )
    )
  }

  test("TaggedDecimal.apply - scale > tag") {
    assertEquals(TaggedDecimal[6, 4](BigDecimal("1.23456")).isRight, true)
  }

  test("TaggedDecimal schema") {
    val schema = AvroSchema[TaggedDecimal[6, 4]]
    assertEquals(schema.getType, Schema.Type.BYTES)
    assertEquals(schema.getLogicalType.getName, "decimal")

    val logical = schema.getLogicalType.asInstanceOf[LogicalTypes.Decimal]
    assertEquals(logical.getPrecision, 6)
    assertEquals(logical.getScale, 4)
  }

  test("TaggedDecimal binary round-trip (scale == tag)") {
    type TD = TaggedDecimal[6, 4]
    val td: TD = TaggedDecimal[6, 4](BigDecimal("12.3456")).getOrElse(sys.error("nope"))

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[TD](AvroSchema[TD], Encoder[TD])
      .to(baos)
      .build()
    out.write(td)
    out.close()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[TD]
      .from(bais)
      .build(AvroSchema[TD])
    val roundTrippedDecimal = in.iterator.next()
    in.close()

    assertEquals(roundTrippedDecimal, td)
  }

  test("TaggedDecimal binary round-trip (scale > tag)") {
    type TD = TaggedDecimal[6, 4]
    val td: TD = TaggedDecimal[6, 4](BigDecimal("1.23456")).getOrElse(sys.error("nope"))

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[TD](AvroSchema[TD], Encoder[TD])
      .to(baos)
      .build()
    out.write(td)
    out.close()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[TD]
      .from(bais)
      .build(AvroSchema[TD])
    val roundTrippedDecimal = in.iterator.next()
    in.close()

    // value has been truncated to 4 dp, rounding up
    assertEquals(roundTrippedDecimal.value, BigDecimal("1.2346"))
  }

  test("TaggedDecimal binary round-trip (scale < tag)") {
    type TD = TaggedDecimal[6, 4]
    val td: TD = TaggedDecimal[6, 4](BigDecimal("1.234")).getOrElse(sys.error("nope"))

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[TD](AvroSchema[TD], Encoder[TD])
      .to(baos)
      .build()
    out.write(td)
    out.close()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[TD]
      .from(bais)
      .build(AvroSchema[TD])
    val roundTrippedDecimal = in.iterator.next()
    in.close()

    assertEquals(roundTrippedDecimal.value, BigDecimal("1.23400"))
  }

}
