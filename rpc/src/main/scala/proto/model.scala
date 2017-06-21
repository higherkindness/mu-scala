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

object model {

  sealed trait ProtoMessageField extends Product with Serializable {
    def name: String
    def id: String
    def tag: Int
    def comment: Option[String]
  }

  final case class ProtoDouble(
      name: String,
      id: String = "double",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFloat(
      name: String,
      id: String = "float",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoInt32(
      name: String,
      id: String = "int32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoInt64(
      name: String,
      id: String = "int64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoUInt32(
      name: String,
      id: String = "uint32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoUInt64(
      name: String,
      id: String = "uint64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSInt32(
      name: String,
      id: String = "sint32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSInt64(
      name: String,
      id: String = "sint64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFInt32(
      name: String,
      id: String = "fixed32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFInt64(
      name: String,
      id: String = "fixed64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSFInt32(
      name: String,
      id: String = "sfixed32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSFInt64(
      name: String,
      id: String = "sfixed64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoBool(
      name: String,
      id: String = "bool",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoString(
      name: String,
      id: String = "string",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoBytes(
      name: String,
      id: String = "bytes",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoEnum(
      name: String,
      id: String = "enum",
      tag: Int,
      values: List[ProtoEnumValue],
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoEnumValue(name: String, tag: Int)

  final case class ProtoCustomType(
      name: String,
      id: String,
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField(name, id, tag, comment)

  final case class ProtoMessage(
      name: String,
      reservedNames: List[String] = Nil,
      reservedTags: List[Int] = Nil,
      fields: List[ProtoMessageField] = Nil)

}
