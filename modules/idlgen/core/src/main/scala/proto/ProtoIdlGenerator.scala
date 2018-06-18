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

package freestyle.rpc.idlgen.proto

import freestyle.rpc.idlgen._
import freestyle.rpc.protocol._
import freestyle.rpc.internal.util.{AstOptics, Toolbox}

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
    "// the idlGen plugin, from frees-rpc service definitions.",
    "// Read more at: http://frees.io/docs/rpc",
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
    val serviceLines = services.flatMap {
      case RpcService(name, requests) => textBlock("service", name, requestFields(requests))
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
      case RpcRequest(_, name, reqType, retType, streamingType) =>
        s"  rpc ${name.capitalize} (${requestType(reqType, streamingType)}) returns (${responseType(retType, streamingType)});"
    }

  private def requestType(t: String, streamingType: Option[StreamingType]): String =
    paramType(t, streamingType, RequestStreaming, BidirectionalStreaming)

  private def responseType(t: String, streamingType: Option[StreamingType]): String =
    paramType(t, streamingType, ResponseStreaming, BidirectionalStreaming)

  private def paramType(
      t: String,
      streamingType: Option[StreamingType],
      matchingStreamingTypes: StreamingType*): String = {
    val pType = if (t == EmptyType) ProtoEmpty else t
    if (streamingType.exists(matchingStreamingTypes.contains)) s"stream $pType" else pType
  }

  private def mappedType(typeArg: Tree): String = typeArg match {
    case ast._Ident(Ident(TypeName("Boolean"))) => "bool"
    case ast._Ident(Ident(TypeName("Int")))     => "int32"
    case ast._Ident(Ident(TypeName("Long")))    => "int64"
    case ast._Ident(Ident(TypeName("Float")))   => "float"
    case ast._Ident(Ident(TypeName("Double")))  => "double"
    case ast._Ident(Ident(TypeName("String")))  => "string"
    case ast._AppliedTypeTree(
        AppliedTypeTree(ast._Ident(Ident(TypeName("Array"))), List(TermName("Byte")))) =>
      "bytes"
    case ast._AppliedTypeTree(AppliedTypeTree(ast._Ident(Ident(TypeName("Option"))), List(t))) =>
      mappedType(t)
    case ast._AppliedTypeTree(AppliedTypeTree(ast._Ident(Ident(TypeName("List"))), List(t))) =>
      s"repeated ${mappedType(t)}"
    case _ => typeArg.toString
  }
}
