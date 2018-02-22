import freestyle.rpc.protocol._

@message
case class Person(id: Long, name: String, email: Option[String])
