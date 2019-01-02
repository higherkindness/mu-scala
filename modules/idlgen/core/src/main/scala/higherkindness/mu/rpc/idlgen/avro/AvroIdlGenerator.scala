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

package higherkindness.mu.rpc.idlgen.avro

import higherkindness.mu.rpc.idlgen._
import higherkindness.mu.rpc.idlgen.util._
import higherkindness.mu.rpc.internal.util._
import higherkindness.mu.rpc.protocol._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object AvroIdlGenerator extends AvroIdlGenerator {
  val serializationType: SerializationType = Avro
}

object AvroWithSchemaIdlGenerator extends AvroIdlGenerator {
  val serializationType: SerializationType = AvroWithSchema
}

trait AvroIdlGenerator extends IdlGenerator {

  import Toolbox.u._
  import Model._
  import AstOptics._
  import StringUtil._

  val idlType: String       = avro.IdlType
  val outputSubdir: String  = "avro"
  val fileExtension: String = AvprExtension

  // Note: don't use the object directly as the implicit value unless moving it here, or circe will give invalid output
  implicit private val avroTypeEncoder: Encoder[AvroType] = AvroTypeEncoder

  override protected def generateFrom(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]): Seq[String] = {

    val avroRecords = messages.map {
      case RpcMessage(name, params) =>
        AvroRecord(
          name = name,
          fields = params map {
            case ast._ValDef(ValDef(_, TermName(name), tpt, _)) =>
              AvroField(name, mappedType(tpt))
          }
        )
    }
    val avroMessages: Map[String, AvroMessage] = services
      .flatMap(_.requests)
      .filter(_.streamingType.isEmpty)
      .map {
        case RpcRequest(name, reqType, respType, _) =>
          name -> AvroMessage(
            Seq(AvroField(DefaultRequestParamName, mappedType(reqType)))
              .filterNot(_.`type` == AvroEmpty),
            mappedType(respType))
      }
      .toMap
    val protocol = AvroProtocol(outputPackage.getOrElse(""), outputName, avroRecords, avroMessages)
    protocol.asJson.spaces2.split('\n')
  }

  case class AvroProtocol(
      namespace: String,
      protocol: String,
      types: Seq[AvroRecord],
      messages: Map[String, AvroMessage])

  case class AvroRecord(name: String, `type`: String = "record", fields: Seq[AvroField])

  case class AvroMessage(request: Seq[AvroField], response: AvroType)

  case class AvroField(name: String, `type`: AvroType)

  sealed trait AvroType
  case object AvroEmpty                     extends AvroType
  case class AvroRef(ref: String)           extends AvroType
  case class AvroArray(elemType: AvroType)  extends AvroType
  case class AvroOption(elemType: AvroType) extends AvroType

  object AvroTypeEncoder extends Encoder[AvroType] {
    def apply(t: AvroType): Json = t match {
      case AvroEmpty            => "null"
      case AvroRef(ref)         => ref
      case AvroArray(elemType)  => Json.obj("type" -> "array", "items" -> apply(elemType))
      case AvroOption(elemType) => Json.arr(apply(AvroEmpty), apply(elemType))
    }
  }

  def mappedType(typeArg: Tree): AvroType = typeArg match {
    case BaseType("Boolean")                => "boolean"
    case BaseType("Int")                    => "int"
    case BaseType("Long")                   => "long"
    case BaseType("Float")                  => "float"
    case BaseType("Double")                 => "double"
    case BaseType("String")                 => "string"
    case SingleAppliedTypeTree("Seq", t)    => AvroArray(mappedType(t))
    case SingleAppliedTypeTree("List", t)   => AvroArray(mappedType(t))
    case SingleAppliedTypeTree("Array", t)  => AvroArray(mappedType(t))
    case SingleAppliedTypeTree("Option", t) => AvroOption(mappedType(t))
    case SingletonType("Empty")             => AvroEmpty
    case _                                  => typeArg.toString.unquoted
  }

  implicit private def string2AvroRef(s: String): AvroRef = AvroRef(s)

  implicit private def string2Json(s: String): Json = s.asJson
}
