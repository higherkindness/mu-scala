/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen

import higherkindness.mu.rpc.protocol.{SerializationType => SerType, _}
import shapeless.tag
import shapeless.tag.@@

object Model {

  import Toolbox.u._

  private implicit class StringOps(val s: String) extends AnyVal {
    def trimAll: String = s.replaceAll("\\s", "")
  }

  case class RpcDefinitions(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]
  )

  case class RpcOption(name: String, value: String)

  case class RpcMessage(name: String, params: Seq[ValDef]) {
    // Workaround for `Term.Param` using referential equality; needed mostly for unit testing
    override def equals(other: Any): Boolean = other match {
      case that: RpcMessage =>
        this.name == that.name && this.params.map(_.toString.trimAll) == that.params.map(
          _.toString.trimAll
        )
      case _ => false
    }
  }

  case class RpcService(
      serializationType: SerType,
      name: String,
      requests: Seq[RpcRequest]
  )

  case class RpcRequest(
      name: String,
      requestType: Tree,
      responseType: Tree,
      streamingType: Option[StreamingType] = None
  ) {
    // Workaround for `Type` using referential equality; needed mostly for unit testing
    override def equals(other: Any): Boolean = other match {
      case that: RpcRequest =>
        this.name == that.name &&
          this.requestType.toString.trimAll == that.requestType.toString.trimAll &&
          this.responseType.toString.trimAll == that.responseType.toString.trimAll &&
          this.streamingType == that.streamingType
      case _ => false
    }
  }

  sealed trait IdlType extends Product with Serializable
  object IdlType {
    case object Proto   extends IdlType
    case object Avro    extends IdlType
    case object OpenAPI extends IdlType
    case object Unknown extends IdlType
  }

  sealed trait SerializationType extends Product with Serializable
  object SerializationType {
    case object Protobuf       extends SerializationType
    case object Avro           extends SerializationType
    case object AvroWithSchema extends SerializationType
    case object Custom         extends SerializationType
  }

  sealed abstract class MarshallersImport(val marshallersImport: String)
      extends Product
      with Serializable

  case class CustomMarshallersImport(mi: String) extends MarshallersImport(mi)
  case object BigDecimalAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigdecimal._")
  case object BigDecimalTaggedAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._")
  case object JavaTimeDateAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.javatime._")
  case object JodaDateTimeAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro._")
  case object BigDecimalProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.pbd.bigdecimal._")
  case object JavaTimeDateProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.pbd.javatime._")
  case object JodaDateTimeProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.marshallers.jodaTimeEncoders.pbd._")

  sealed trait BigDecimalTypeGen       extends Product with Serializable
  case object ScalaBigDecimalGen       extends BigDecimalTypeGen
  case object ScalaBigDecimalTaggedGen extends BigDecimalTypeGen

  sealed abstract class CompressionTypeGen(val value: String) extends Product with Serializable
  case object GzipGen                                         extends CompressionTypeGen("Gzip")
  case object NoCompressionGen                                extends CompressionTypeGen("Identity")

  trait UseIdiomaticEndpointsTag
  type UseIdiomaticEndpoints = Boolean @@ UseIdiomaticEndpointsTag
  object UseIdiomaticEndpoints {
    val trueV = UseIdiomaticEndpoints(true)

    def apply(v: Boolean): UseIdiomaticEndpoints =
      tag[UseIdiomaticEndpointsTag][Boolean](v)
  }

  sealed trait StreamingImplementation
  case object Fs2Stream       extends StreamingImplementation
  case object MonixObservable extends StreamingImplementation

}
