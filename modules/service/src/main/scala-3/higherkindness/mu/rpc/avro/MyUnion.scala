package higherkindness.mu.rpc.avro

sealed trait MyUnion
case class One(a: Int)    extends MyUnion
case class Two(b: String) extends MyUnion
