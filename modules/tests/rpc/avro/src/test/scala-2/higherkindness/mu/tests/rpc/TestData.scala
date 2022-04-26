package higherkindness.mu.tests.rpc

import shapeless._

object TestData {

  val request = Request(
    a = 123,
    b = tag[(Nat._8, Nat._4)](BigDecimal("3.456")),
    c = shapeless.Coproduct[Long :+: String :+: Boolean :+: Foo :+: CNil](Foo("wow"))
  )

  val requestWithEnumField = RequestWithEnumField(
    a = 123,
    b = tag[(Nat._8, Nat._4)](BigDecimal("3.456")),
    c = shapeless.Coproduct[Long :+: String :+: Boolean :+: Foo :+: CNil](Foo("wow")),
    d = Direction.SOUTH
  )

}
