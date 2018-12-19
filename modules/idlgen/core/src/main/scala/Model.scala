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

package higherkindness.mu.rpc.idlgen

import higherkindness.mu.rpc.internal.util.Toolbox
import higherkindness.mu.rpc.internal.util.StringUtil._
import higherkindness.mu.rpc.protocol._

object Model {

  import Toolbox.u._

  case class RpcDefinitions(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService])

  case class RpcOption(name: String, value: String)

  case class RpcMessage(name: String, params: Seq[ValDef]) {
    // Workaround for `Term.Param` using referential equality; needed mostly for unit testing
    override def equals(other: Any): Boolean = other match {
      case that: RpcMessage =>
        this.name == that.name && this.params.map(_.toString.trimAll) == that.params.map(
          _.toString.trimAll)
      case _ => false
    }
  }

  case class RpcService(
      serializationType: SerializationType,
      name: String,
      requests: Seq[RpcRequest])

  case class RpcRequest(
      name: String,
      requestType: Tree,
      responseType: Tree,
      streamingType: Option[StreamingType] = None) {
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

  sealed abstract class MarshallersImport(val marshallersImport: String)
      extends Product
      with Serializable

  final case class CustomMarshallersImport(mi: String) extends MarshallersImport(mi)
  case object BigDecimalAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigdecimal._")
  case object BigDecimalTaggedAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._")
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
}
