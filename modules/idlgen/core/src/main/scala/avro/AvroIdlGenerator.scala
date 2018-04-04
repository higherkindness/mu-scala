/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc.idlgen.avro

import freestyle.rpc.idlgen._
import freestyle.rpc.protocol._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import scala.meta._

object AvroIdlGenerator extends AvroIdlGenerator { val serializationType: SerializationType = Avro }
object AvroWithSchemaIdlGenerator extends AvroIdlGenerator {
  val serializationType: SerializationType = AvroWithSchema
}

trait AvroIdlGenerator extends IdlGenerator {

  val idlType: String                      = avro.IdlType
  val outputSubdir: String                 = "avro"
  val fileExtension: String                = AvprExtension

  // Note: don't use the object directly as the implicit value unless moving it here, or circe will give invalid output
  implicit private val avroTypeEncoder: Encoder[AvroType] = AvroTypeEncoder

  protected def generateFrom(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]): Seq[String] = {

    val avroRecords = messages.map {
      case RpcMessage(name, params) =>
        AvroRecord(name = name, fields = params.flatMap {
          case param"$name: $tpe" => tpe.map(t => AvroField(name.toString, mappedType(t)))
        })
    }
    val avroMessages = services
      .flatMap(_.requests)
      .filter(_.streamingType.isEmpty)
      .map {
        case RpcRequest(_, name, reqType, respType, _) =>
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

  private def mappedType(typeArg: Type.Arg): AvroType = typeArg match {
    case targ"Boolean"    => "boolean"
    case targ"Int"        => "int"
    case targ"Long"       => "long"
    case targ"Float"      => "float"
    case targ"Double"     => "double"
    case targ"String"     => "string"
    case targ"Seq[$t]"    => AvroArray(mappedType(t))
    case targ"List[$t]"   => AvroArray(mappedType(t))
    case targ"Array[$t]"  => AvroArray(mappedType(t))
    case targ"Option[$t]" => AvroOption(mappedType(t))
    case targ"Empty.type" => AvroEmpty
    case _                => typeArg.toString
  }

  implicit private def string2AvroRef(s: String): AvroRef = AvroRef(s)

  implicit private def string2Json(s: String): Json = s.asJson
}
