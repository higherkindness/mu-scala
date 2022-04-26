package higherkindness.mu.tests.rpc

import higherkindness.mu.rpc.avro.AvroUnion4
import higherkindness.mu.rpc.avro.Decimals.TaggedDecimal

object TestData {

  val request = Request(
    a = 123,
    b = TaggedDecimal[8, 4](BigDecimal("3.456")).getOrElse(throw new RuntimeException("nope!")),
    c = AvroUnion4[Long, String, Boolean, Foo](Foo("wow"))
  )

  val requestWithEnumField = RequestWithEnumField(
    a = 123,
    b = TaggedDecimal[8, 4](BigDecimal("3.456")).getOrElse(throw new RuntimeException("nope!")),
    c = AvroUnion4[Long, String, Boolean, Foo](Foo("wow")),
    d = Direction.SOUTH
  )

}
