/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle
package rpc
package protocol

import freestyle.rpc.protocol.model._
import simulacrum.typeclass

object encoders {

  @typeclass
  trait ProtoEncoder[A] {
    def encode(a: A): String
  }

  object ProtoEncoder {

    implicit def defaultProtoMessageFieldEncoder(
        implicit PME: ProtoEncoder[ProtoFieldMod]): ProtoEncoder[ProtoMessageField] =
      new ProtoEncoder[ProtoMessageField] {
        override def encode(a: ProtoMessageField): String = a match {
          case m: ProtoEnum =>
            s"""
               |enum ${m.id} {
               |  ${m.values.zipWithIndex.map { case (v, i) => s"${v.name} = $i" }.mkString(";\n")}
               |}
               |${a.mod.fold("")(m => PME.encode(m) + " ")}${a.id} ${a.name} = ${a.tag};
         """.stripMargin
          case _ => s"${a.id} ${a.name} = ${a.tag};"
        }
      }

    implicit def defaultProtoMessageEncoder(
        implicit MFEncoder: ProtoEncoder[ProtoMessageField]): ProtoEncoder[ProtoMessage] =
      new ProtoEncoder[ProtoMessage] {
        override def encode(a: ProtoMessage): String =
          s"""
             |message ${a.name} {
             |${a.fields.map(MFEncoder.encode).mkString("   ", "\n   ", "")}
             |}
           """.stripMargin
      }

    implicit def defaultProtoMessageFieldModEncoder: ProtoEncoder[ProtoFieldMod] =
      new ProtoEncoder[ProtoFieldMod] {
        override def encode(a: ProtoFieldMod): String = a match {
          case Repeated => "repeated"
          case Optional => "optional"
          case Required => "required"
        }
      }

    implicit def defaultProtoServiceEncoder(
        implicit MFEncoder: ProtoEncoder[ProtoServiceField]): ProtoEncoder[ProtoService] =
      new ProtoEncoder[ProtoService] {
        override def encode(a: ProtoService): String =
          s"""
             |service ${a.name} {
             |${a.rpcs.map(MFEncoder.encode).mkString("   ", "\n   ", "")}
             |}
           """.stripMargin
      }

    implicit def defaultProtoServiceFieldEncoder: ProtoEncoder[ProtoServiceField] =
      new ProtoEncoder[ProtoServiceField] {
        override def encode(a: ProtoServiceField): String = a.streamingType match {
          case None =>
            s"rpc ${a.name} (${a.request.capitalize}) returns (${a.response.capitalize}) {}"
          case Some(RequestStreaming) =>
            s"rpc ${a.name} (stream ${a.request.capitalize}) returns (${a.response.capitalize}) {}"
          case Some(ResponseStreaming) =>
            s"rpc ${a.name} (${a.request.capitalize}) returns (stream ${a.response.capitalize}) {}"
          case Some(BidirectionalStreaming) =>
            s"rpc ${a.name} (stream ${a.request.capitalize}) returns (stream ${a.response.capitalize}) {}"
        }

      }

  }

}
