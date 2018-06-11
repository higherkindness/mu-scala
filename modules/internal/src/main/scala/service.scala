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

  //todo: move the Context-dependent inner classes and functions elsewhere, if possible
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
      private val rpcDefs: List[DefDef] = defs.collect {
        case d: DefDef if findAnnotation(d.mods, "rpc").isDefined => d
      }
      private val rpcRequests: List[RpcRequest] = rpcDefs.map { d =>
        val name   = d.name
        val params = d.vparamss.flatten
        require(params.length == 1, s"RPC call $name has more than one request parameter")
        RpcRequest(name, params.head.tpt, d.tpt, findAnnotation(d.mods, "rpc").get.children.tail)
      }
      private val nonRpcDefs: List[Tree] = defs.collect {
        case d: DefDef if findAnnotation(d.mods, "rpc").isEmpty => d
      }
      val imports: List[Tree] = defs.collect {
        case imp: Import => imp
      }

      val methodDescriptors: List[Tree] = rpcRequests.map(_.methodDescriptor)
      private val serverCallDescriptorsAndHandlers: List[Tree] =
        rpcRequests.map(_.descriptorAndHandler)
      val bindService: Tree =
        q"""
        def bindService[$F_](implicit
          F: _root_.cats.effect.Effect[$F],
          algebra: $serviceName[$F],
          S: _root_.monix.execution.Scheduler
        ): _root_.io.grpc.ServerServiceDefinition =
          new _root_.freestyle.rpc.internal.service.GRPCServiceDefBuilder(${lit(serviceName)}, ..$serverCallDescriptorsAndHandlers).apply
        """

      private val clientCallMethods: List[Tree] = rpcRequests.map(_.clientDef)
      private val Client                        = TypeName("Client")
      //todo: surpressWarts("DefaultArguments")
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

      //todo: surpressWarts("DefaultArguments")
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

      //todo: surpressWarts("DefaultArguments")
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

      //todo: validate that the request and responses are case classes, if possible
      case class RpcRequest(
          methodName: TermName,
          requestType: Tree,
          responseType: Tree,
          options: List[Tree]) {

        private val serializationType: SerializationType =
          annotationParam(options, 0, "serializationType") match {
            case "Protobuf"       => Protobuf
            case "Avro"           => Avro
            case "AvroWithSchema" => AvroWithSchema
          }

        private val compressionType: CompressionType =
          annotationParam(options, 1, "compressionType", Some("Identity")) match {
            case "Identity" => Identity
            case "Gzip"     => Gzip
          }

        private val requestStreamingImpl: Option[StreamingImpl]  = streamingImplFor(requestType)
        private val responseStreamingImpl: Option[StreamingImpl] = streamingImplFor(responseType)
        private val streamingImpls: Set[StreamingImpl] =
          Set(requestStreamingImpl, responseStreamingImpl).flatten
        require(
          streamingImpls.size < 2,
          s"RPC service $serviceName has different streaming implementations for request and response")
        private val streamingImpl: StreamingImpl =
          streamingImpls.headOption.getOrElse(MonixObservable)

        private val streamingType: Option[StreamingType] =
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

        private val clientCallsImpl = streamingImpl match {
          case Fs2Stream       => q"_root_.freestyle.rpc.internal.client.fs2Calls"
          case MonixObservable => q"_root_.freestyle.rpc.internal.client.monixCalls"
        }

        private val serverCallsImpl = streamingImpl match {
          case Fs2Stream       => q"_root_.freestyle.rpc.internal.server.fs2Calls"
          case MonixObservable => q"_root_.freestyle.rpc.internal.server.monixCalls"
        }

        private val streamingMethodType = {
          val suffix = streamingType match {
            case Some(RequestStreaming)       => "CLIENT_STREAMING"
            case Some(ResponseStreaming)      => "SERVER_STREAMING"
            case Some(BidirectionalStreaming) => "BIDI_STREAMING"
            case None                         => "UNARY"
          }
          q"_root_.io.grpc.MethodDescriptor.MethodType.${TermName(suffix)}"
        }

        //todo: surpressWarts("Null", "ExplicitImplicitTypes")
        private val encodersImport = serializationType match {
          case Protobuf =>
            q"import _root_.freestyle.rpc.internal.encoders.pbd._"
          case Avro =>
            q"import _root_.freestyle.rpc.internal.encoders.avro._"
          case AvroWithSchema =>
            q"import _root_.freestyle.rpc.internal.encoders.avrowithschema._"
        }

        private val methodDescriptorName = TermName(methodName + "MethodDescriptor")

        private val reqType = requestType match {
          case tq"$s[..$tpts]" if requestStreamingImpl.isDefined => tpts.last
          case other                                             => other
        }
        private val respType = responseType match {
          case tq"$x[..$tpts]" => tpts.last
        }
        //todo: surpressWarts("DefaultArguments")
        val methodDescriptor: Tree = q"""
          val $methodDescriptorName: _root_.io.grpc.MethodDescriptor[$reqType, $respType] = {
          $encodersImport
          _root_.io.grpc.MethodDescriptor
            .newBuilder(
              implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$reqType]],
              implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$respType]])
            .setType($streamingMethodType)
            .setFullMethodName(
              _root_.io.grpc.MethodDescriptor.generateFullMethodName(${lit(serviceName)}, ${lit(
          methodName)}))
            .build()
          }
        """

        private def clientCallMethodFor(clientMethodName: String) =
          q"$clientCallsImpl.${TermName(clientMethodName)}(input, $methodDescriptorName, channel, options)"

        val clientDef: Tree = streamingType match {
          case Some(RequestStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallMethodFor(
              "clientStreaming")}"""
          case Some(ResponseStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallMethodFor(
              "serverStreaming")}"""
          case Some(BidirectionalStreaming) =>
            q"""
            def $methodName(input: $requestType): $responseType = ${clientCallMethodFor(
              "bidiStreaming")}"""
          case None => q"""
            def $methodName(input: $requestType): $responseType = ${clientCallMethodFor("unary")}"""
        }

        private val compressionOption = compressionType match {
          case Identity => q"None"
          case Gzip     => q"""Some("gzip")"""
        }

        private def serverCallMethodFor(serverMethodName: String) =
          q"$serverCallsImpl.${TermName(serverMethodName)}(algebra.$methodName, $compressionOption)"

        val descriptorAndHandler: Tree = {
          val handler = streamingType match {
            case Some(RequestStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(${serverCallMethodFor("clientStreamingMethod")})"
            case Some(ResponseStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(${serverCallMethodFor("serverStreamingMethod")})"
            case Some(BidirectionalStreaming) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(${serverCallMethodFor("bidiStreamingMethod")})"
            case None =>
              q"_root_.io.grpc.stub.ServerCalls.asyncUnaryCall(${serverCallMethodFor("unaryMethod")})"
          }
          q"($methodDescriptorName, $handler)"
        }
      }
    }
    /* todo: restore with a proper scalamacros replacement to `mod`, if possible
    private def surpressWarts(warts: String*) = {
      val wartsString = warts.toList.map(w => "org.wartremover.warts." + w)
      val argList     = wartsString.map(ws => arg"$ws")
      mod"""@root_.java.lang.SuppressWarnings(_root_.scala.Array(..$argList))"""
    }*/

    val classAndMaybeCompanion = annottees.map(_.tree)
    val result: List[Tree] = classAndMaybeCompanion.head match {
      case serviceDef: ClassDef
          if serviceDef.mods.hasFlag(TRAIT) || serviceDef.mods.hasFlag(ABSTRACT) =>
        val service = new RpcService(serviceDef)
        val companion: ModuleDef = classAndMaybeCompanion.lastOption match {
          case Some(obj: ModuleDef) => obj
          case _ =>
            ModuleDef(
              NoMods,
              serviceDef.name.toTermName,
              Template(
                List(TypeTree(typeOf[AnyRef])),
                noSelfType,
                List(
                  DefDef(
                    Modifiers(),
                    termNames.CONSTRUCTOR,
                    List(),
                    List(List()),
                    TypeTree(),
                    Block(List(pendingSuperCall), Literal(Constant(())))))
              )
            )
        }
        //todo: surpressWarts("Any", "NonUnitStatements", "StringPlusAny", "Throw")
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
    c.Expr(Block(result, Literal(Constant(()))))
  }
}

sealed trait StreamingImpl  extends Product with Serializable
case object Fs2Stream       extends StreamingImpl
case object MonixObservable extends StreamingImpl

// $COVERAGE-ON$
