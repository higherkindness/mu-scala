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

  sealed trait ProtoFieldMod extends Product with Serializable

  case object Repeated extends ProtoFieldMod

  sealed trait ProtoMessageField extends Product with Serializable {
    def mod: Option[ProtoFieldMod]

    def name: String

    def id: String

    def tag: Int

    def comment: Option[String]
  }

  final case class ProtoDouble(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "double",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFloat(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "float",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoInt32(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "int32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoInt64(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "int64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoUInt32(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "uint32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoUInt64(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "uint64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSInt32(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "sint32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSInt64(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "sint64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFInt32(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "fixed32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoFInt64(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "fixed64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSFInt32(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "sfixed32",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoSFInt64(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "sfixed64",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoBool(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "bool",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoString(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "string",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoBytes(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "bytes",
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoEnum(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String = "enum",
      tag: Int,
      values: List[ProtoEnumValue],
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoEnumValue(mod: Option[ProtoFieldMod] = None, name: String, tag: Int)

  final case class ProtoCustomType(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      id: String,
      tag: Int,
      comment: Option[String] = None)
      extends ProtoMessageField

  final case class ProtoMessage(
      mod: Option[ProtoFieldMod] = None,
      name: String,
      reservedNames: List[String] = Nil,
      reservedTags: List[Int] = Nil,
      fields: List[ProtoMessageField] = Nil)

  final case class ProtoService(name: String, rpcs: List[ProtoServiceField])

  final case class ProtoServiceField(
      name: String,
      request: String,
      response: String,
      streamingType: Option[StreamingType])

  final case class ProtoOption(name: String, value: String, quote: Boolean)

  final case class ProtoDefinitions(
      prelude: String = """|// This file has been automatically generated for use by
                           |// sbt-frees-protogen plugin, from freestyle-rpc service definitions
                           |
                           |syntax = "proto3";""".stripMargin,
      options: List[ProtoOption],
      messages: List[ProtoMessage],
      services: List[ProtoService])

}
