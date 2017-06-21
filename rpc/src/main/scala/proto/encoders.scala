package freestyle
package rpc
package protocol

import freestyle.rpc.protocol.model.{ProtoEnum, ProtoMessageField}

object encoders {

  trait ProtoMessageEncoder[A] {
    def encode(a: A): String
  }

  object ProtoMessageEncoder extends Instances {

    def apply[A <: ProtoMessageField](a: A)(implicit E: ProtoMessageEncoder[A]): String =
      E.encode(a)

  }

  trait Instances extends LowPriorityInstances {

    implicit val enumMessageFieldEncoder: ProtoMessageEncoder[ProtoEnum] =
      new ProtoMessageEncoder[ProtoEnum] {
        override def encode(a: ProtoEnum): String =
          s"""
           |enum ${a.id} {
           |  ${a.values.zipWithIndex.map { case (v, i) => s"${v.name} = ${i}" }.mkString(";\n")}
           |}
           |${a.id} ${a.name} = 4;
         """.stripMargin
      }

  }

  trait LowPriorityInstances {

    implicit def defaultProtoMessageFieldEncoder[A <: ProtoMessageField] =
      new ProtoMessageEncoder[A] {
        override def encode(a: A): String = s"${a.id} ${a.name} = ${a.tag}"
      }

  }

}
