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

import scala.meta.Defn.Class
import scala.meta._

object converters {

  trait ScalaMetaClass2ProtoMessage {
    def convert(c: Class): ProtoMessage
  }

  object ScalaMetaClass2ProtoMessage {
    implicit def defaultClass2MessageConverter(implicit PC: ScalaMetaParam2ProtoMessageField) =
      new ScalaMetaClass2ProtoMessage {
        override def convert(c: Class): ProtoMessage = ProtoMessage(
          name = c.name.value,
          fields =
            c.ctor.paramss.flatten.zipWithIndex.map { case (p, t) => PC.convert(p, t + 1) }.toList
        )
      }
  }

  trait ScalaMetaParam2ProtoMessageField {
    def convert(p: Term.Param, tag: Int): ProtoMessageField
  }

  object ScalaMetaParam2ProtoMessageField {
    implicit def defaultParam2ProtoMessageField: ScalaMetaParam2ProtoMessageField =
      new ScalaMetaParam2ProtoMessageField {
        override def convert(p: Term.Param, tag: Int): ProtoMessageField = p match {
          case param"..$mods $paramname: Double = $expropt" =>
            ProtoDouble(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Float = $expropt" =>
            ProtoFloat(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Long = $expropt" =>
            ProtoInt64(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Boolean = $expropt" =>
            ProtoBool(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Int = $expropt" =>
            ProtoInt32(name = paramname.value, tag = tag)
          case param"..$mods $paramname: String = $expropt" =>
            ProtoString(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Array[Byte] = $expropt" =>
            ProtoBytes(name = paramname.value, tag = tag)
          case param"..$mods $paramname: $tpe = $expropt" =>
            ProtoCustomType(name = paramname.value, tag = tag, id = tpe.toString())
        }
      }
  }
}
