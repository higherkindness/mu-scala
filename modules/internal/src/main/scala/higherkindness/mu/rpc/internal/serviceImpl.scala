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

package higherkindness.mu.rpc
package internal

import higherkindness.mu.rpc.protocol._

import scala.reflect.macros.blackbox

// $COVERAGE-OFF$
object serviceImpl {

  //todo: move the Context-dependent inner classes and functions elsewhere, if possible
  def service(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    abstract class TypeTypology(tpe: Tree, inner: Option[Tree], isStreaming: Boolean)
        extends Product
        with Serializable {
      def getTpe: Tree           = tpe
      def getInner: Option[Tree] = inner
      def streaming: Boolean     = isStreaming
      def safeInner: Tree        = inner.getOrElse(tpe)
    }
    object TypeTypology {
      def apply(t: Tree): TypeTypology = t match {
        case tq"Observable[..$tpts]"       => MonixObservable(t, tpts.headOption)
        case tq"Stream[$carrier, ..$tpts]" => Fs2Stream(t, tpts.headOption)
        case tq"Empty.type"                => EmptyTpe(t)
        case tq"$carrier[..$tpts]"         => Unary(t, tpts.headOption)
      }
    }
    case class EmptyTpe(tpe: Tree)                       extends TypeTypology(tpe, None, false)
    case class Unary(tpe: Tree, inner: Option[Tree])     extends TypeTypology(tpe, inner, false)
    case class Fs2Stream(tpe: Tree, inner: Option[Tree]) extends TypeTypology(tpe, inner, true)
    case class MonixObservable(tpe: Tree, inner: Option[Tree])
        extends TypeTypology(tpe, inner, true)

    case class Operation(name: TermName, request: TypeTypology, response: TypeTypology) {

      val isStreaming: Boolean = request.streaming || response.streaming

      val streamingType: Option[StreamingType] = (request.streaming, response.streaming) match {
        case (true, true)  => Some(BidirectionalStreaming)
        case (true, false) => Some(RequestStreaming)
        case (false, true) => Some(ResponseStreaming)
        case _             => None
      }

      val validStreamingComb: Boolean = (request, response) match {
        case (Fs2Stream(_, _), MonixObservable(_, _)) => false
        case (MonixObservable(_, _), Fs2Stream(_, _)) => false
        case _                                        => true
      }

      val isMonixObservable: Boolean = List(request, response).collect {
        case m: MonixObservable => m
      }.nonEmpty

      require(
        validStreamingComb,
        s"RPC service $name has different streaming implementations for request and response")

      val prevalentStreamingTarget: TypeTypology = streamingType match {
        case Some(ResponseStreaming) => response
        case _                       => request
      }

    }

    trait SupressWarts[T] {
      def supressWarts(warts: String*)(t: T): T
    }

    object SupressWarts {
      def apply[A](implicit A: SupressWarts[A]): SupressWarts[A] = A

      implicit val supressWartsOnModifier: SupressWarts[Modifiers] = new SupressWarts[Modifiers] {
        def supressWarts(warts: String*)(mod: Modifiers): Modifiers = {
          val argList = warts.map(ws => s"org.wartremover.warts.$ws")

          Modifiers(
            mod.flags,
            mod.privateWithin,
            q"new _root_.java.lang.SuppressWarnings(_root_.scala.Array(..$argList))" :: mod.annotations)
        }
      }

      implicit val supressWartsOnClassDef: SupressWarts[ClassDef] = new SupressWarts[ClassDef] {
        def supressWarts(warts: String*)(clazz: ClassDef): ClassDef = {
          ClassDef(
            SupressWarts[Modifiers].supressWarts(warts: _*)(clazz.mods),
            clazz.name,
            clazz.tparams,
            clazz.impl
          )
        }
      }

      implicit val supressWartsOnDefDef: SupressWarts[DefDef] = new SupressWarts[DefDef] {
        def supressWarts(warts: String*)(defdef: DefDef): DefDef = {
          DefDef(
            SupressWarts[Modifiers].supressWarts(warts: _*)(defdef.mods),
            defdef.name,
            defdef.tparams,
            defdef.vparamss,
            defdef.tpt,
            defdef.rhs
          )
        }
      }

      implicit val supressWartsOnValDef: SupressWarts[ValDef] = new SupressWarts[ValDef] {
        def supressWarts(warts: String*)(valdef: ValDef): ValDef = {
          ValDef(
            SupressWarts[Modifiers].supressWarts(warts: _*)(valdef.mods),
            valdef.name,
            valdef.tpt,
            valdef.rhs
          )
        }
      }

      class PimpedSupressWarts[A](value: A)(implicit A: SupressWarts[A]) {
        def supressWarts(warts: String*): A = A.supressWarts(warts: _*)(value)
      }

      implicit def pimpSupressWarts[A: SupressWarts](a: A) = new PimpedSupressWarts(a)
    }

    import SupressWarts._

    class RpcService(serviceDef: ClassDef) {
      val serviceName: TypeName = serviceDef.name

      require(
        serviceDef.tparams.length == 1,
        s"@service-annotated class $serviceName must have a single type parameter")

      val F_ : TypeDef = serviceDef.tparams.head
      val F: TypeName  = F_.name

      private val defs: List[Tree] = serviceDef.impl.body

      private val (rpcDefs, nonRpcDefs) = defs.collect {
        case d: DefDef => d
      } partition (_.rhs.isEmpty)

      private def compressionType(anns: List[Tree]): Tree =
        annotationParam(anns, 1, "compressionType", Some("Identity")) match {
          case "Identity" => q"None"
          case "Gzip"     => q"""Some("gzip")"""
        }

      private val rpcRequests: List[RpcRequest] = for {
        d      <- rpcDefs
        params <- d.vparamss
        _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
        p <- params.headOption.toList
        operation = Operation(d.name, TypeTypology(p.tpt), TypeTypology(d.tpt))
      } yield RpcRequest(operation, compressionType(serviceDef.mods.annotations))

      val imports: List[Tree] = defs.collect {
        case imp: Import => imp
      }

      private val serializationType: SerializationType =
        c.prefix.tree match {
          case q"new service($serializationType)" =>
            serializationType.toString match {
              case "Protobuf"       => Protobuf
              case "Avro"           => Avro
              case "AvroWithSchema" => AvroWithSchema
              case _ =>
                sys.error(
                  "@service annotation should have a SerializationType parameter [Protobuf|Avro|AvroWithSchema]")
            }
          case q"new service($serializationType, $_)" =>
            serializationType.toString match {
              case "Protobuf"       => Protobuf
              case "Avro"           => Avro
              case "AvroWithSchema" => AvroWithSchema
              case _ =>
                sys.error(
                  "@service annotation should have a SerializationType parameter [Protobuf|Avro|AvroWithSchema], and a CompressionType parameter [Identity|Gzip]")
            }
          case _ =>
            sys.error(
              "@service annotation should have a SerializationType parameter [Protobuf|Avro|AvroWithSchema]")
        }

      val encodersImport = serializationType match {
        case Protobuf =>
          List(q"import _root_.higherkindness.mu.rpc.internal.encoders.pbd._")
        case Avro =>
          List(q"import _root_.higherkindness.mu.rpc.internal.encoders.avro._")
        case AvroWithSchema =>
          List(q"import _root_.higherkindness.mu.rpc.internal.encoders.avrowithschema._")
      }

      val methodDescriptors: List[Tree] = rpcRequests.map(_.methodDescriptor)
      private val serverCallDescriptorsAndHandlers: List[Tree] =
        rpcRequests.map(_.descriptorAndHandler)
      val bindService: DefDef = q"""
        def bindService[$F_](implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          algebra: $serviceName[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ): $F[_root_.io.grpc.ServerServiceDefinition] =
          _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[$F](${lit(
        serviceName)}, ..$serverCallDescriptorsAndHandlers)
        """

      private val clientCallMethods: List[Tree] = rpcRequests.map(_.clientDef)
      private val Client                        = TypeName("Client")
      val clientClass: ClassDef =
        q"""
        class $Client[$F_](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ) extends _root_.io.grpc.stub.AbstractStub[$Client[$F]](channel, options) with $serviceName[$F] {
          override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $Client[$F] =
              new $Client[$F](channel, options)

          ..$clientCallMethods
          ..$nonRpcDefs
        }""".supressWarts("DefaultArguments")

      val client: DefDef =
        q"""
        def client[$F_](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(
            _root_.higherkindness.mu.rpc.channel.UsePlaintext()),
            options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
          )(implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ): _root_.cats.effect.Resource[F, $serviceName[$F]] =
          _root_.cats.effect.Resource.make {
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).build
          }(channel => F.void(F.delay(channel.shutdown()))).flatMap(ch =>
          _root_.cats.effect.Resource.make[F, $serviceName[$F]](F.delay(new $Client[$F](ch, options)))(_ => F.unit))
        """.supressWarts("DefaultArguments")

      val clientFromChannel: DefDef =
        q"""
        def clientFromChannel[$F_](
          channel: F[_root_.io.grpc.ManagedChannel],
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ): _root_.cats.effect.Resource[F, $serviceName[$F]] = _root_.cats.effect.Resource.make(channel)(channel =>
        F.void(F.delay(channel.shutdown()))).flatMap(ch =>
        _root_.cats.effect.Resource.make[F, $serviceName[$F]](F.delay(new $Client[$F](ch, options)))(_ => F.unit))
        """.supressWarts("DefaultArguments")

      val unsafeClient: DefDef =
        q"""
        def unsafeClient[$F_](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(
            _root_.higherkindness.mu.rpc.channel.UsePlaintext()),
            options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
          )(implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ): $serviceName[$F] = {
          val managedChannelInterpreter =
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).unsafeBuild
          new $Client[$F](managedChannelInterpreter, options)
        }""".supressWarts("DefaultArguments")

      val unsafeClientFromChannel: DefDef =
        q"""
        def unsafeClientFromChannel[$F_](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(implicit
          F: _root_.cats.effect.ConcurrentEffect[$F],
          EC: _root_.scala.concurrent.ExecutionContext
        ): $serviceName[$F] = new $Client[$F](channel, options)
        """.supressWarts("DefaultArguments")

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
        mods.annotations find {
          case Apply(Select(New(Ident(TypeName(`name`))), _), _)     => true
          case Apply(Select(New(Select(_, TypeName(`name`))), _), _) => true
          case _                                                     => false
        }

      //todo: validate that the request and responses are case classes, if possible
      case class RpcRequest(
          operation: Operation,
          compressionOption: Tree
      ) {

        private val clientCallsImpl = operation.prevalentStreamingTarget match {
          case _: Fs2Stream       => q"_root_.higherkindness.mu.rpc.internal.client.fs2Calls"
          case _: MonixObservable => q"_root_.higherkindness.mu.rpc.internal.client.monixCalls"
          case _                  => q"_root_.higherkindness.mu.rpc.internal.client.unaryCalls"
        }

        private val streamingMethodType = {
          val suffix = operation.streamingType match {
            case Some(RequestStreaming)       => "CLIENT_STREAMING"
            case Some(ResponseStreaming)      => "SERVER_STREAMING"
            case Some(BidirectionalStreaming) => "BIDI_STREAMING"
            case None                         => "UNARY"
          }
          q"_root_.io.grpc.MethodDescriptor.MethodType.${TermName(suffix)}"
        }

        private val methodDescriptorName = TermName(operation.name + "MethodDescriptor")

        private val reqType = operation.request.safeInner

        private val respType = operation.response.safeInner

        val methodDescriptor: DefDef = q"""
          def $methodDescriptorName(implicit
            ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[$reqType],
            RespM: _root_.io.grpc.MethodDescriptor.Marshaller[$respType]
          ): _root_.io.grpc.MethodDescriptor[$reqType, $respType] = {
            _root_.io.grpc.MethodDescriptor
              .newBuilder(
                ReqM,
                RespM)
              .setType($streamingMethodType)
              .setFullMethodName(
                _root_.io.grpc.MethodDescriptor.generateFullMethodName(${lit(serviceName)}, ${lit(
          operation.name)}))
              .build()
          }
        """.supressWarts("Null", "ExplicitImplicitTypes")

        private def clientCallMethodFor(clientMethodName: String) =
          q"$clientCallsImpl.${TermName(clientMethodName)}(input, $methodDescriptorName, channel, options)"

        val clientDef: Tree = operation.streamingType match {
          case Some(RequestStreaming) =>
            q"""
            def ${operation.name}(input: ${operation.request.getTpe}): ${operation.response.getTpe} = ${clientCallMethodFor(
              "clientStreaming")}"""
          case Some(ResponseStreaming) =>
            q"""
            def ${operation.name}(input: ${operation.request.getTpe}): ${operation.response.getTpe} = ${clientCallMethodFor(
              "serverStreaming")}"""
          case Some(BidirectionalStreaming) =>
            q"""
            def ${operation.name}(input: ${operation.request.getTpe}): ${operation.response.getTpe} = ${clientCallMethodFor(
              "bidiStreaming")}"""
          case None =>
            q"""
            def ${operation.name}(input: ${operation.request.getTpe}): ${operation.response.getTpe} = ${clientCallMethodFor(
              "unary")}"""
        }

        private def serverCallMethodFor(serverMethodName: String) =
          q"_root_.higherkindness.mu.rpc.internal.server.monixCalls.${TermName(serverMethodName)}(algebra.${operation.name}, $compressionOption)"

        val descriptorAndHandler: Tree = {
          val handler = (operation.streamingType, operation.prevalentStreamingTarget) match {
            case (Some(RequestStreaming), Fs2Stream(_, _)) =>
              q"_root_.higherkindness.mu.rpc.internal.server.fs2Calls.clientStreamingMethod(algebra.${operation.name}, $compressionOption)"
            case (Some(RequestStreaming), MonixObservable(_, _)) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(${serverCallMethodFor("clientStreamingMethod")})"
            case (Some(ResponseStreaming), Fs2Stream(_, _)) =>
              q"_root_.higherkindness.mu.rpc.internal.server.fs2Calls.serverStreamingMethod(algebra.${operation.name}, $compressionOption)"
            case (Some(ResponseStreaming), MonixObservable(_, _)) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(${serverCallMethodFor("serverStreamingMethod")})"
            case (Some(BidirectionalStreaming), Fs2Stream(_, _)) =>
              q"_root_.higherkindness.mu.rpc.internal.server.fs2Calls.bidiStreamingMethod(algebra.${operation.name}, $compressionOption)"
            case (Some(BidirectionalStreaming), MonixObservable(_, _)) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(${serverCallMethodFor("bidiStreamingMethod")})"
            case (None, _) =>
              q"_root_.io.grpc.stub.ServerCalls.asyncUnaryCall(_root_.higherkindness.mu.rpc.internal.server.unaryCalls.unaryMethod(algebra.${operation.name}, $compressionOption))"
            case _ =>
              sys.error(
                s"Unable to define a handler for the streaming type ${operation.streamingType} and ${operation.prevalentStreamingTarget} for the method ${operation.name} in the service $serviceName")
          }
          q"($methodDescriptorName, $handler)"
        }
      }

      //----------
      // HTTP/REST
      //----------
      //TODO: derive server as well
      //TODO: move HTTP-related code to its own module (on last attempt this did not work)

      case class HttpOperation(operation: Operation) {

        import operation._

        val uri = name.toString

        val method: TermName = request match {
          case _: EmptyTpe => TermName("GET")
          case _           => TermName("POST")
        }

        val executionClient: Tree = response match {
          case MonixObservable(_, _) =>
            q"client.stream(request).flatMap(_.asStream[${response.safeInner}]).toObservable"
          case Fs2Stream(_, _) =>
            q"client.stream(request).flatMap(_.asStream[${response.safeInner}])"
          case _ =>
            q"client.expectOr[${response.safeInner}](request)(handleResponseError)"
        }

        val requestTypology: Tree = request match {
          case _: Unary =>
            q"val request = Request[F](Method.$method, uri / ${uri.replace("\"", "")}).withEntity(req.asJson)"
          case _: Fs2Stream =>
            q"val request = Request[F](Method.$method, uri / ${uri.replace("\"", "")}).withEntity(req.map(_.asJson))"
          case _: MonixObservable =>
            q"val request = Request[F](Method.$method, uri / ${uri.replace("\"", "")}).withEntity(req.toFs2Stream.map(_.asJson))"
          case _ =>
            q"val request = Request[F](Method.$method, uri / ${uri.replace("\"", "")})"
        }

        val responseEncoder =
          q"""implicit val responseDecoder: EntityDecoder[F, ${response.safeInner}] = jsonOf[F, ${response.safeInner}]"""

        def toTree: Tree = request match {
          case _: EmptyTpe =>
            q"""def $name(implicit client: _root_.org.http4s.client.Client[F]): ${response.getTpe} = {
		                  $responseEncoder
		                  $requestTypology
		                  $executionClient
		                 }"""
          case _ =>
            q"""def $name(req: ${request.getTpe})(implicit client: _root_.org.http4s.client.Client[F]): ${response.getTpe} = {
		                  $responseEncoder
		                  $requestTypology
		                  $executionClient
		                 }"""
        }

      }

      val operations: List[HttpOperation] = for {
        d      <- rpcDefs.collect { case x if findAnnotation(x.mods, "http").isDefined => x }
        args   <- findAnnotation(d.mods, "http").collect({ case Apply(_, args) => args }).toList
        params <- d.vparamss
        _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
        p <- params.headOption.toList
      } yield HttpOperation(Operation(d.name, TypeTypology(p.tpt), TypeTypology(d.tpt)))

      val httpRequests = operations.map(_.toTree)

      val HttpClient                = TypeName("HttpClient")
      val httpClientClass: ClassDef = q"""
        class $HttpClient[$F_](uri: Uri)(implicit F: _root_.cats.effect.ConcurrentEffect[$F], ec: scala.concurrent.ExecutionContext) {
          ..$httpRequests
      }"""

      val httpClient: DefDef = q"""
        def httpClient[$F_](uri: Uri)
          (implicit F: _root_.cats.effect.ConcurrentEffect[$F], ec: scala.concurrent.ExecutionContext): $HttpClient[$F] = {
          new $HttpClient[$F](uri)
      }"""

      val httpImports: List[Tree] = List(
        q"import _root_.higherkindness.mu.rpc.http.Utils._",
        q"import _root_.org.http4s._",
        q"import _root_.org.http4s.circe._",
        q"import _root_.io.circe._",
        q"import _root_.io.circe.generic.auto._",
        q"import _root_.io.circe.syntax._"
      )

      val scheduler: List[Tree] = operations
        .find(_.operation.isMonixObservable)
        .map(_ => q"import _root_.monix.execution.Scheduler.Implicits.global")
        .toList

      val http =
        if (httpRequests.isEmpty) Nil
        else
          httpImports ++ scheduler ++ List(httpClientClass, httpClient)
    }

    val classAndMaybeCompanion = annottees.map(_.tree)
    val result: List[Tree] = classAndMaybeCompanion.head match {
      case serviceDef: ClassDef
          if serviceDef.mods.hasFlag(TRAIT) || serviceDef.mods.hasFlag(ABSTRACT) => {
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

        val enrichedCompanion = ModuleDef(
          companion.mods.supressWarts("Any", "NonUnitStatements", "StringPlusAny", "Throw"),
          companion.name,
          Template(
            companion.impl.parents,
            companion.impl.self,
            companion.impl.body ++ service.imports ++ service.methodDescriptors ++ service.encodersImport ++ List(
              service.bindService,
              service.clientClass,
              service.client,
              service.clientFromChannel,
              service.unsafeClient,
              service.unsafeClientFromChannel
            ) ++ service.http
          )
        )

        List(serviceDef, enrichedCompanion)
      }
      case _ => sys.error("@service-annotated definition must be a trait or abstract class")
    }
    c.Expr(Block(result, Literal(Constant(()))))
  }

}
// $COVERAGE-ON$
