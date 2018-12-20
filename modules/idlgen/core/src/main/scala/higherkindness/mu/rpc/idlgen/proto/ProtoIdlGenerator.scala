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

package higherkindness.mu.rpc.idlgen.proto

import higherkindness.mu.rpc.idlgen._
import higherkindness.mu.rpc.protocol._
import higherkindness.mu.rpc.internal.util.{AstOptics, Toolbox}

object ProtoIdlGenerator extends IdlGenerator {

  import Toolbox.u._
  import Model._
  import AstOptics._

  val idlType: String                      = IdlType
  val serializationType: SerializationType = Protobuf
  val outputSubdir: String                 = "proto"
  val fileExtension: String                = ProtoExtension

  private val HeaderLines = Seq(
    "// This file has been automatically generated for use by",
    "// the idlGen plugin, from mu service definitions.",
    "// Read more at: https://higherkindness.github.io/mu/scala/",
    "",
    "syntax = \"proto3\";",
    ""
  )

  protected def generateFrom(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]): Seq[String] = {

    val packageLines = outputPackage.map(pkg => Seq(s"package $pkg;", "")).getOrElse(Seq.empty)

    val optionLines = options.map {
      case RpcOption(name, value) => s"option $name = $value;"
    } :+ ""
    val messageLines = messages.flatMap {
      case RpcMessage(name, params) => textBlock("message", name, messageFields(params)) :+ ""
    }
    val serviceLines: Seq[String] = services.flatMap {
      case RpcService(_, name, requests) =>
        textBlock("service", name, requestFields(requests))
    }
    val importLines =
      if (serviceLines.exists(_.contains(ProtoEmpty)))
        Seq("import \"google/protobuf/empty.proto\";", "")
      else Seq.empty

    HeaderLines ++ packageLines ++ importLines ++ optionLines ++ messageLines ++ serviceLines
  }

  private def textBlock(blockType: String, name: String, contents: Seq[String]) =
    s"$blockType $name {" +: contents :+ "}"

  private def messageFields(params: Seq[ValDef]): Seq[String] =
    params
      .map {
        case ast._ValDef(ValDef(_, TermName(name), tpt, _)) => s"  ${mappedType(tpt)} $name"
      }
      .zipWithIndex
      .map { case (field, i) => s"$field = ${i + 1};" }

  private def requestFields(requests: Seq[RpcRequest]): Seq[String] =
    requests.map {
      case RpcRequest(name, reqType, retType, streamingType) =>
        s"  rpc ${name.capitalize} (${requestType(reqType, streamingType)}) returns (${responseType(retType, streamingType)});"
    }

  private def requestType(t: Tree, streamingType: Option[StreamingType]): String =
    paramType(t, streamingType, RequestStreaming, BidirectionalStreaming)

  private def responseType(t: Tree, streamingType: Option[StreamingType]): String =
    paramType(t, streamingType, ResponseStreaming, BidirectionalStreaming)

  private def paramType(
      tpe: Tree,
      streamingType: Option[StreamingType],
      matchingStreamingTypes: StreamingType*): String = {
    val t     = tpe.toString
    val pType = if (t == EmptyType) ProtoEmpty else t
    if (streamingType.exists(matchingStreamingTypes.contains)) s"stream $pType" else pType
  }

  private def mappedType(typeArg: Tree): String = typeArg match {
    case BaseType("Boolean")                              => "bool"
    case BaseType("Int")                                  => "int32"
    case BaseType("Long")                                 => "int64"
    case BaseType("Float")                                => "float"
    case BaseType("Double")                               => "double"
    case BaseType("String")                               => "string"
    case SingleAppliedTypeTree("Array", TermName("Byte")) => "bytes"
    case SingleAppliedTypeTree("Option", t)               => mappedType(t)
    case SingleAppliedTypeTree("List", t)                 => s"repeated ${mappedType(t)}"
    case _                                                => typeArg.toString
  }
}
