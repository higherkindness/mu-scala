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

package freestyle.rpc
package internal

import freestyle.rpc.protocol._
import scala.reflect.macros.blackbox

// $COVERAGE-OFF$
object serviceImpl {

  def service(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    class RpcService(serviceDef: ClassDef) {
      val serviceName: TypeName = serviceDef.name

      require(
        serviceDef.tparams.length == 1,
        s"@service-annotated class $serviceName must have a single type parameter")

      val F_ : TypeDef = serviceDef.tparams.head
      val F: TypeName  = F_.name

      private val defs: List[Tree] = serviceDef.impl.body
      val rpcDefs: List[DefDef] = defs.collect {
        case d: DefDef if findAnnotation(d.mods, "rpc").isDefined => d
      }
      val rpcCalls: List[RpcRequest] = rpcDefs.map { d =>
        val name   = d.name
        val params = d.vparamss.flatten
        require(params.length == 1, s"RPC call $name has more than one request parameter")
        RpcRequest(name, params.head.tpt, d.tpt, findAnnotation(d.mods, "rpc").get.children.tail)
      }
      val nonRpcDefs: List[Tree] = defs.collect {
        case d: DefDef if findAnnotation(d.mods, "rpc").isEmpty => d
      }
      val imports: List[Tree] = defs.collect {
        case imp: Import => imp
      }

      val methodDescriptors: List[Tree]             = rpcCalls.map(_.methodDescriptor)
      private val serverCallDescriptors: List[Tree] = rpcCalls.map(_.descriptorAndHandler)
      val bindService: Tree =
        q"""
        def bindService[$F_](implicit
          F: _root_.cats.effect.Effect[$F],
          algebra: $serviceName[$F],
          S: _root_.monix.execution.Scheduler
        ): _root_.io.grpc.ServerServiceDefinition =
          new _root_.freestyle.rpc.internal.service.GRPCServiceDefBuilder(${lit(serviceName)}, ..$serverCallDescriptors).apply
        """

      private val clientCallMethods: List[Tree] = rpcCalls.map(_.clientDef)
      private val Client                        = TypeName("Client")
      val clientClass: Tree =
        q"""
        class $Client[$F_](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(implicit
          F: _root_.cats.effect.Effect[$F],
          S: _root_.monix.execution.Scheduler
        ) extends _root_.io.grpc.stub.AbstractStub[$Client[$F]](channel, options) {
          override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $Client[F] =
              new $Client[$F](channel, options)
          ..$clientCallMethods
          ..$nonRpcDefs
        }"""

      val client: Tree =
        q"""
        def client[$F_](
          channelFor: _root_.freestyle.rpc.ChannelFor,
          channelConfigList: List[_root_.freestyle.rpc.client.ManagedChannelConfig] = List(
            _root_.freestyle.rpc.client.UsePlaintext()),
            options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
          )(implicit
          F: _root_.cats.effect.Effect[$F],
          S: _root_.monix.execution.Scheduler
        ): $Client[$F] = {
          val managedChannelInterpreter =
            new _root_.freestyle.rpc.client.ManagedChannelInterpreter[F](channelFor, channelConfigList)
          new $Client[$F](managedChannelInterpreter.build(channelFor, channelConfigList), options)
        }"""

      val clientFromChannel: Tree =
        q"""
        def clientFromChannel[$F_](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(implicit
          F: _root_.cats.effect.Effect[$F],
          S: _root_.monix.execution.Scheduler
        ): $Client[$F] = new $Client[$F](channel, options)
        """

      private def lit(x: Any): Literal = Literal(Constant(x.toString))

      private def annotationParam(
          params: List[Tree],
          pos: Int,
          name: String,
          default: Option[String] = None): String =
        params
          .collectFirst {
            case q"$pName = $pValue" if pName.toString == name => pValue.toString
          }
          .getOrElse(if (params.isDefinedAt(pos)) params(pos).toString
          else default.getOrElse(sys.error(s"Missing annotation parameter $name")))

      private def findAnnotation(mods: Modifiers, name: String): Option[Tree] =
        mods.annotations.find(_.children.head.toString == s"new $name")

      case class RpcRequest(
          methodName: TermName,
          requestType: Tree,
          responseType: Tree,
          options: List[Tree]) {

        val serializationType: SerializationType =
          annotationParam(options, 0, "serializationType") match {
            case "Protobuf"       => Protobuf
            case "Avro"           => Avro
            case "AvroWithSchema" => AvroWithSchema
          }

        val compressionType: CompressionType =
          annotationParam(options, 1, "compressionType", Some("Identity")) match {
            case "Identity" => Identity
            case "Gzip"     => Gzip
          }

        val requestStreamingImpl: Option[StreamingImpl]  = streamingImplFor(requestType)
        val responseStreamingImpl: Option[StreamingImpl] = streamingImplFor(responseType)
        val streamingImpls: Set[StreamingImpl] =
          Set(requestStreamingImpl, responseStreamingImpl).flatten
        require(
          streamingImpls.size < 2,
          s"RPC service $serviceName has different streaming implementations for request and response")
        val streamingImpl: StreamingImpl = streamingImpls.headOption.getOrElse(MonixObservable)

        val streamingType: Option[StreamingType] =
          if (requestStreamingImpl.isDefined && responseStreamingImpl.isDefined)
            Some(BidirectionalStreaming)
          else if (requestStreamingImpl.isDefined) Some(RequestStreaming)
          else if (responseStreamingImpl.isDefined) Some(ResponseStreaming)
          else None

        private def streamingImplFor(t: Tree): Option[StreamingImpl] = t match {
          case tq"$tpt[..$tpts]" if tpt.toString.endsWith("Observable") => Some(MonixObservable)
          case tq"$tpt[..$tpts]" if tpt.toString.endsWith("Stream")     => Some(Fs2Stream)
          case _                                                        => None
        }

        val clientCall = streamingImpl match {
          case Fs2Stream       => q"_root_.freestyle.rpc.internal.client.fs2Calls"
          case MonixObservable => q"_root_.freestyle.rpc.internal.client.monixCalls"
        }

        val serverCall = streamingImpl match {
          case Fs2Stream       => q"_root_.freestyle.rpc.internal.server.fs2Calls"
          case MonixObservable => q"_root_.freestyle.rpc.internal.server.monixCalls"
        }

        val methodType = {
          val suffix = streamingType match {
            case Some(RequestStreaming)       => "CLIENT_STREAMING"
            case Some(ResponseStreaming)      => "SERVER_STREAMING"
            case Some(BidirectionalStreaming) => "BIDI_STREAMING"
            case None                         => "UNARY"
          }
          q"_root_.io.grpc.MethodDescriptor.MethodType.${TermName(suffix)}"
        }

        val encodersImport = serializationType match {
          case Protobuf =>
            q"import _root_.freestyle.rpc.internal.encoders.pbd._"
          case Avro =>
            q"import _root_.freestyle.rpc.internal.encoders.avro._"
          case AvroWithSchema =>
            q"import _root_.freestyle.rpc.internal.encoders.avrowithschema._"
        }

        val descriptorName = TermName(methodName + "MethodDescriptor")

        val reqType = requestType match {
          case tq"$s[..$tpts]" if requestStreamingImpl.isDefined => tpts.last
          case other => other
        }
        val respType = responseType  match {
          case tq"$x[..$tpts]" => tpts.last
        }
        val methodDescriptor = q"""
          val $descriptorName: _root_.io.grpc.MethodDescriptor[$reqType, $respType] = {
          $encodersImport
          _root_.io.grpc.MethodDescriptor
            .newBuilder(
              implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$reqType]],
              implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$respType]])
            .setType($methodType)
            .setFullMethodName(
              _root_.io.grpc.MethodDescriptor.generateFullMethodName(${lit(serviceName)}, ${lit(
          methodName)}))
            .build()
          }
        """

        def clientCallType(clientMethodName: String) =
          q"$clientCall.${TermName(clientMethodName)}(input, $descriptorName, channel, options)"

        val clientDef = streamingType match {
          case Some(RequestStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallType(
              "clientStreaming")}"""
          case Some(ResponseStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallType(
              "serverStreaming")}"""
          case Some(BidirectionalStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallType("bidiStreaming")}"""
          case None => q"""
            def $methodName(input: $requestType): $responseType = ${clientCallType("unary")}"""
        }

        val maybeAlg = compressionType match {
          case Identity => q"None"
          case Gzip     => q"""Some("gzip")"""
        }

        def serverCallType(serverMethodName: String) =
          q"$serverCall.${TermName(serverMethodName)}(algebra.$methodName, $maybeAlg)"

        val descriptorAndHandler: Tree = {
          val handler = streamingType match {
            case Some(RequestStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(${serverCallType("clientStreamingMethod")})"
            case Some(ResponseStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(${serverCallType("serverStreamingMethod")})"
            case Some(BidirectionalStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(${serverCallType("bidiStreamingMethod")})"
            case None =>
              q"_root_.io.grpc.stub.ServerCalls.asyncUnaryCall(${serverCallType("unaryMethod")})"
          }
          q"($descriptorName, $handler)"
        }
      }
    }

    val classAndMaybeCompanion = annottees.map(_.tree)
    val result: List[Tree] = classAndMaybeCompanion.head match {
      case serviceDef: ClassDef
          if serviceDef.mods.hasFlag(TRAIT) || serviceDef.mods.hasFlag(ABSTRACT) =>
        val service = new RpcService(serviceDef)
        val companion: ModuleDef = classAndMaybeCompanion.lastOption match {
          case Some(obj: ModuleDef) => obj
          case _ =>
            ModuleDef(
              Modifiers(),
              serviceDef.name.toTermName,
              Template(List(TypeTree(typeOf[AnyRef])), noSelfType, Nil))
        }
        val enrichedCompanion = ModuleDef(
          companion.mods,
          companion.name,
          Template(
            companion.impl.parents,
            companion.impl.self,
            companion.impl.body ++ service.imports ++ service.methodDescriptors ++ List(
              service.bindService,
              service.clientClass,
              service.client,
              service.clientFromChannel
            )
          )
        )
        List(serviceDef, enrichedCompanion)
      case _ => sys.error("@service-annotated definition must be a trait or abstract class")
    }
    println(result) //todo: remove this
    c.Expr(Block(result, Literal(Constant(()))))
  }
}

sealed trait StreamingImpl  extends Product with Serializable
case object Fs2Stream       extends StreamingImpl
case object MonixObservable extends StreamingImpl

// $COVERAGE-ON$
