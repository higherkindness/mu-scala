/*
 * Copyright 2017-2020 47 Degrees <http://47deg.com>
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

package higherkindness.mu.rpc.internal

import higherkindness.mu.rpc.internal.service.makro._
import higherkindness.mu.rpc.protocol._
import scala.reflect.macros.blackbox

// $COVERAGE-OFF$
class serviceImpl(val c: blackbox.Context) {

  import c.universe._

  val wartSuppression = new WartSuppression[c.type](c)
  import wartSuppression._

  def service(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import Flag._

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
                    Block(List(pendingSuperCall), Literal(Constant(())))
                  )
                )
              )
            )
        }

        val enrichedCompanion = ModuleDef(
          companion.mods.suppressWarts("Any", "NonUnitStatements", "StringPlusAny", "Throw"),
          companion.name,
          Template(
            companion.impl.parents,
            companion.impl.self,
            companion.impl.body ++ service.imports ++ service.encodersImport ++ service.methodDescriptors ++ List(
              service.bindService,
              service.bindTracingService,
              service.clientClass,
              service.client,
              service.clientFromChannel,
              service.unsafeClient,
              service.unsafeClientFromChannel,
              service.tracingClientClass,
              service.tracingClient,
              service.tracingClientFromChannel,
              service.unsafeTracingClient,
              service.unsafeTracingClientFromChannel
            ) ++ service.http
          )
        )

        List(serviceDef, enrichedCompanion)
      case _ => sys.error("@service-annotated definition must be a trait or abstract class")
    }

    c.Expr(Block(result, Literal(Constant(()))))
  }

  /**
   * @originalType The original type written by the user.
   *               For a request type, this will be e.g. `MyRequest` or `Stream[F, MyRequest]`
   *               or `Observable[MyRequest]`.
   *               For a response type, it will be inside an effect type,
   *               e.g. `F[MyResponse]` or `F[Stream[F, MyResponse]]` or `F[Observable[MyResponse]]`
   *
   * @unwrappedType The type, with any surrounding effect type stripped.
   *                e.g. `MyRequest` or `Stream[F, MyRequest]` or `Observable[MyRequest]`
   *                For a request type, `unwrappedType` == `originalType`.
   *
   * @messageType The type of the message in a request/response.
   *              For non-streaming request/responses, `messageType` == `unwrappedType`.
   *              For streaming request/responses, `messageType` is the type of the stream elements.
   *              e.g. if `originalType` is `F[Stream[F, MyResponse]]` then `messageType` is `MyResponse`.
   */
  abstract class TypeTypology(
      val originalType: Tree,
      val unwrappedType: Tree,
      val messageType: Tree
  ) extends Product
      with Serializable {

    def isEmpty: Boolean = this match {
      case _: EmptyTpe => true
      case _           => false
    }

    def isStreaming: Boolean = this match {
      case _: Fs2StreamTpe       => true
      case _: MonixObservableTpe => true
      case _                     => false
    }
  }
  object TypeTypology {

    /**
     * Extract the parts of a possibly-qualified type name or term name.
     *
     * {{{
     * extractName(tq"Observable") == List("Observable")
     * extractName(tq"monix.reactive.Observable") == List("monix", "reactive", "Observable")
     * }}}
     */
    private def extractName(tree: Tree): List[String] = tree match {
      case Ident(TermName(name))             => List(name)
      case Ident(TypeName(name))             => List(name)
      case Select(qualifier, TypeName(name)) => extractName(qualifier) :+ name
      case Select(qualifier, TermName(name)) => extractName(qualifier) :+ name
      case _                                 => Nil
    }

    /**
     * Is the given tree a type name or term name that matches the given
     * fully-qualified name?
     *
     * {{{
     * val fqn = List("_root_", "monix", "reactive", "Observable")
     *
     * possiblyQualifiedName(tq"Observable", fqn) == true
     * possiblyQualifiedName(tq"reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"monix.reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"_root_.monix.reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"com.mylibrary.Observable", fqn) == false
     * }}}
     */
    private def possiblyQualifiedName(tree: Tree, fqn: List[String]): Boolean = {
      val name = extractName(tree)
      name.nonEmpty && fqn.endsWith(name)
    }

    private val monixObservableFQN = List("_root_", "monix", "reactive", "Observable")
    private val fs2StreamFQN       = List("_root_", "fs2", "Stream")

    def apply(t: Tree, responseType: Boolean, F: TypeName): TypeTypology = {
      val unwrappedType: Tree = {
        if (responseType) {
          // Check that the type is wrapped in F as it should be, and unwrap it
          t match {
            case tq"$f[$tparam]" if f.toString == F.decodedName.toString => tparam
            case _ =>
              c.abort(
                t.pos,
                "Invalid RPC response type. All response types should have the shape F[...], where F[_] is the service's type parameter."
              )
          }
        } else {
          // Request type is not wrapped in F[...], so return it as-is
          t
        }
      }

      unwrappedType match {
        case tq"$tpe[$elemType]" if possiblyQualifiedName(tpe, monixObservableFQN) =>
          MonixObservableTpe(t, unwrappedType, elemType)
        case tq"$tpe[$effectType, $elemType]"
            if possiblyQualifiedName(tpe, fs2StreamFQN) && effectType.toString == F.decodedName.toString =>
          Fs2StreamTpe(t, unwrappedType, elemType)
        case tq"Empty.type" => EmptyTpe(t, unwrappedType, unwrappedType)
        case other          => UnaryTpe(t, unwrappedType, unwrappedType)
      }

    }

  }

  case class EmptyTpe(orig: Tree, unwrapped: Tree, message: Tree)
      extends TypeTypology(orig, unwrapped, message)

  case class UnaryTpe(orig: Tree, unwrapped: Tree, message: Tree)
      extends TypeTypology(orig, unwrapped, message)

  case class Fs2StreamTpe(orig: Tree, unwrapped: Tree, streamElem: Tree)
      extends TypeTypology(orig, unwrapped, streamElem)

  case class MonixObservableTpe(orig: Tree, unwrapped: Tree, streamElem: Tree)
      extends TypeTypology(orig, unwrapped, streamElem)

  case class Operation(name: TermName, request: TypeTypology, response: TypeTypology) {

    val isStreaming: Boolean = request.isStreaming || response.isStreaming

    val streamingType: Option[StreamingType] = (request.isStreaming, response.isStreaming) match {
      case (true, true)  => Some(BidirectionalStreaming)
      case (true, false) => Some(RequestStreaming)
      case (false, true) => Some(ResponseStreaming)
      case _             => None
    }

    val validStreamingComb: Boolean = (request, response) match {
      case (_: Fs2StreamTpe, _: MonixObservableTpe) => false
      case (_: MonixObservableTpe, _: Fs2StreamTpe) => false
      case _                                        => true
    }

    require(
      validStreamingComb,
      s"RPC service $name has different streaming implementations for request and response"
    )

    val isMonixObservable: Boolean = List(request, response).collect {
      case m: MonixObservableTpe => m
    }.nonEmpty

    val prevalentStreamingTarget: TypeTypology =
      if (streamingType.contains(ResponseStreaming)) response else request

  }

  class RpcService(serviceDef: ClassDef) {
    val serviceName: TypeName = serviceDef.name

    require(
      serviceDef.tparams.length == 1,
      s"@service-annotated class $serviceName must have a single type parameter"
    )

    val F_ : TypeDef = serviceDef.tparams.head
    val F: TypeName  = F_.name

    require(
      F_.tparams.length == 1,
      s"@service-annotated class $serviceName's type parameter must be higher-kinded"
    )

    // Type lambda for Kleisli[F, Span[F], *]
    private val kleisliFSpanF =
      tq"({ type T[α] = _root_.cats.data.Kleisli[$F, _root_.natchez.Span[$F], α] })#T"

    private def kleisliFSpanFB(B: Tree) =
      tq"_root_.cats.data.Kleisli[$F, _root_.natchez.Span[$F], $B]"

    private val defs: List[Tree] = serviceDef.impl.body

    private val (rpcDefs, nonRpcDefs) = defs.collect {
      case d: DefDef => d
    } partition (_.rhs.isEmpty)

    val annotationParams: List[Either[String, (String, String)]] = c.prefix.tree match {
      case q"new service(..$seq)" =>
        seq.toList.map {
          case q"$pName = $pValue" => Right((pName.toString(), pValue.toString()))
          case param               => Left(param.toString())
        }
      case _ => Nil
    }

    private val compressionType: CompressionType =
      annotationParam(1, "compressionType") {
        case "Gzip"     => Gzip
        case "Identity" => Identity
      }.getOrElse(Identity)

    private val OptionString = """Some\("(.+)"\)""".r

    private val namespacePrefix: String =
      annotationParam(2, "namespace") {
        case OptionString(s) => s"$s."
        case "None"          => ""
      }.getOrElse("")

    private val fullServiceName = namespacePrefix + serviceName.toString

    private val methodNameStyle: MethodNameStyle =
      annotationParam(3, "methodNameStyle") {
        case "Capitalize" => Capitalize
        case "Unchanged"  => Unchanged
      }.getOrElse(Unchanged)

    private val rpcRequests: List[RpcRequest] = for {
      d      <- rpcDefs
      params <- d.vparamss
      _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
      p <- params.headOption.toList
    } yield {
      val requestType  = TypeTypology(p.tpt, false, F)
      val responseType = TypeTypology(d.tpt, true, F)
      RpcRequest(
        Operation(d.name, requestType, responseType),
        compressionType,
        methodNameStyle
      )
    }

    val imports: List[Tree] = defs.collect {
      case imp: Import => imp
    }

    private val serializationType: SerializationType =
      annotationParam(0, "serializationType") {
        case "Protobuf"       => Protobuf
        case "Avro"           => Avro
        case "AvroWithSchema" => AvroWithSchema
        case "Custom"         => Custom
      }.getOrElse(
        sys.error(
          "@service annotation should have a SerializationType parameter [Protobuf|Avro|AvroWithSchema|Custom]"
        )
      )

    val encodersImport = serializationType match {
      case Protobuf =>
        List(q"import _root_.higherkindness.mu.rpc.internal.encoders.pbd._")
      case Avro =>
        List(q"import _root_.higherkindness.mu.rpc.internal.encoders.avro._")
      case AvroWithSchema =>
        List(q"import _root_.higherkindness.mu.rpc.internal.encoders.avrowithschema._")
      case Custom =>
        List.empty
    }

    val methodDescriptors: List[Tree] = rpcRequests.map(_.methodDescriptorObj)

    private val serverCallDescriptorsAndHandlers: List[Tree] =
      rpcRequests.map(_.descriptorAndHandler)

    val ceImplicit: Tree        = q"CE: _root_.cats.effect.ConcurrentEffect[$F]"
    val csImplicit: Tree        = q"CS: _root_.cats.effect.ContextShift[$F]"
    val schedulerImplicit: Tree = q"S: _root_.monix.execution.Scheduler"

    val bindImplicits: List[Tree] = ceImplicit :: q"algebra: $serviceName[$F]" :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val classImplicits: List[Tree] = ceImplicit :: csImplicit :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val bindService: DefDef = q"""
      def bindService[$F_](implicit ..$bindImplicits): $F[_root_.io.grpc.ServerServiceDefinition] =
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[$F](
          ${lit(fullServiceName)},
          ..$serverCallDescriptorsAndHandlers
        )
      """

    private val serverCallDescriptorsAndTracingHandlers: List[Tree] =
      rpcRequests.map(_.descriptorAndTracingHandler)

    val tracingAlgebra = q"algebra: $serviceName[$kleisliFSpanF]"
    val bindTracingServiceImplicits: List[Tree] = ceImplicit :: tracingAlgebra :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val bindTracingService: DefDef = q"""
      def bindTracingService[$F_](entrypoint: _root_.natchez.EntryPoint[$F])
                                 (implicit ..$bindTracingServiceImplicits): $F[_root_.io.grpc.ServerServiceDefinition] =
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[$F](
          ${lit(fullServiceName)},
          ..$serverCallDescriptorsAndTracingHandlers
        )
      """

    private val clientCallMethods: List[Tree] = rpcRequests.map(_.clientDef)
    private val Client                        = TypeName("Client")
    val clientClass: ClassDef =
      q"""
      class $Client[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits) extends _root_.io.grpc.stub.AbstractStub[$Client[$F]](channel, options) with $serviceName[$F] {
        override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $Client[$F] =
            new $Client[$F](channel, options)

        ..$clientCallMethods
        ..$nonRpcDefs
      }""".suppressWarts("DefaultArguments")

    /*
     * When you write an anonymous parameter in an anonymous function that
     * ignores the parameter, e.g. `List(1, 2, 3).map(_ => "hello")` the
     * -Wunused:params scalac flag does not warn you about it.  That's
     *  because the compiler attaches a `NoWarnAttachment` to the tree for
     *  the parameter.
     *
     * But if you write the same thing in a quasiquote inside a macro, the
     * attachment does not get added, so you get false-positive compiler
     * warnings at the macro use site like: "parameter value x$2 in anonymous
     * function is never used".
     *
     * (The parameter needs a name, even though the function doesn't
     * reference it, so `_` gets turned into a fresh name e.g. `x$2`.  The
     * same thing happens even if you're not in a macro.)
     *
     * I'd say this is a bug in Scala. We work around it by manually adding
     * the attachment.
     */
    private def anonymousParam: ValDef = {
      val tree: ValDef = q"{(_) => 1}".vparams.head
      c.universe.internal.updateAttachment(
        tree,
        c.universe.asInstanceOf[scala.reflect.internal.StdAttachments].NoWarnAttachment
      )
      tree
    }

    val client: DefDef =
      q"""
      def client[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[F, $serviceName[$F]] =
        _root_.cats.effect.Resource.make(
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).build
        )(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[F, $serviceName[$F]](
            CE.delay(new $Client[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val clientFromChannel: DefDef =
      q"""
      def clientFromChannel[$F_](
        channel: $F[_root_.io.grpc.ManagedChannel],
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[$F, $serviceName[$F]] =
        _root_.cats.effect.Resource.make(channel)(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[$F, $serviceName[$F]](
            CE.delay(new $Client[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val unsafeClient: DefDef =
      q"""
      def unsafeClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$F] = {
        val managedChannelInterpreter =
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).unsafeBuild
        new $Client[$F](managedChannelInterpreter, options)
      }""".suppressWarts("DefaultArguments")

    val unsafeClientFromChannel: DefDef =
      q"""
      def unsafeClientFromChannel[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$F] = new $Client[$F](channel, options)
      """.suppressWarts("DefaultArguments")

    private val tracingClientCallMethods: List[Tree] = rpcRequests.map(_.tracingClientDef)
    private val TracingClient                        = TypeName("TracingClient")
    val tracingClientClass: ClassDef =
      q"""
      class $TracingClient[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits) extends _root_.io.grpc.stub.AbstractStub[$TracingClient[$F]](channel, options) with $serviceName[$kleisliFSpanF] {
        override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $TracingClient[$F] =
            new $TracingClient[$F](channel, options)

        ..$tracingClientCallMethods
        ..$nonRpcDefs
      }""".suppressWarts("DefaultArguments")

    val tracingClient: DefDef =
      q"""
      def tracingClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[F, $serviceName[$kleisliFSpanF]] =
        _root_.cats.effect.Resource.make(
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).build
        )(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[F, $serviceName[$kleisliFSpanF]](
            CE.delay(new $TracingClient[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val tracingClientFromChannel: DefDef =
      q"""
      def tracingClientFromChannel[$F_](
        channel: $F[_root_.io.grpc.ManagedChannel],
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[$F, $serviceName[$kleisliFSpanF]] =
        _root_.cats.effect.Resource.make(channel)(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[$F, $serviceName[$kleisliFSpanF]](
            CE.delay(new $TracingClient[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val unsafeTracingClient: DefDef =
      q"""
      def unsafeTracingClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$kleisliFSpanF] = {
        val managedChannelInterpreter =
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).unsafeBuild
        new $TracingClient[$F](managedChannelInterpreter, options)
      }""".suppressWarts("DefaultArguments")

    val unsafeTracingClientFromChannel: DefDef =
      q"""
      def unsafeTracingClientFromChannel[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$kleisliFSpanF] = new $TracingClient[$F](channel, options)
      """.suppressWarts("DefaultArguments")

    private def lit(x: Any): Literal = Literal(Constant(x.toString))

    private def annotationParam[A](pos: Int, name: String)(
        pf: PartialFunction[String, A]
    ): Option[A] = {

      def findNamed: Option[Either[String, (String, String)]] =
        annotationParams.find(_.exists(_._1 == name))

      def findIndexed: Option[Either[String, (String, String)]] =
        annotationParams.lift(pos).filter(_.isLeft)

      (findNamed orElse findIndexed).map(_.fold(identity, _._2)).map { s =>
        pf.lift(s).getOrElse(sys.error(s"Invalid `$name` annotation value ($s)"))
      }
    }

    private def findAnnotation(mods: Modifiers, name: String): Option[Tree] =
      mods.annotations find {
        case Apply(Select(New(Ident(TypeName(`name`))), _), _)     => true
        case Apply(Select(New(Select(_, TypeName(`name`))), _), _) => true
        case _                                                     => false
      }

    //todo: validate that the request and responses are case classes, if possible
    case class RpcRequest(
        operation: Operation,
        compressionType: CompressionType,
        methodNameStyle: MethodNameStyle
    ) {

      import operation._

      private val compressionTypeTree: Tree =
        q"_root_.higherkindness.mu.rpc.protocol.${TermName(compressionType.toString)}"

      private val clientCallsImpl = prevalentStreamingTarget match {
        case _: Fs2StreamTpe       => q"_root_.higherkindness.mu.rpc.internal.client.fs2.calls"
        case _: MonixObservableTpe => q"_root_.higherkindness.mu.rpc.internal.client.monix.calls"
        case _                     => q"_root_.higherkindness.mu.rpc.internal.client.calls"
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

      private val updatedName = methodNameStyle match {
        case Unchanged  => name.toString
        case Capitalize => name.toString.capitalize
      }

      private val methodDescriptorName = TermName(s"${updatedName}MethodDescriptor")

      private val methodDescriptorDefName = TermName("methodDescriptor")

      private val methodDescriptorValName = TermName("_methodDescriptor")

      private val reqType           = request.originalType
      private val reqElemType       = request.messageType
      private val wrappedRespType   = response.originalType
      private val unwrappedRespType = response.unwrappedType
      private val respElemType      = response.messageType

      val methodDescriptorDef: DefDef = q"""
        def $methodDescriptorDefName(implicit
          ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[$reqElemType],
          RespM: _root_.io.grpc.MethodDescriptor.Marshaller[$respElemType]
        ): _root_.io.grpc.MethodDescriptor[$reqElemType, $respElemType] = {
          _root_.io.grpc.MethodDescriptor
            .newBuilder(
              ReqM,
              RespM)
            .setType($streamingMethodType)
            .setFullMethodName(
              _root_.io.grpc.MethodDescriptor.generateFullMethodName(
        ${lit(fullServiceName)}, ${lit(updatedName)}))
            .build()
        }
      """.suppressWarts("Null", "ExplicitImplicitTypes")

      val methodDescriptorVal: ValDef = q"""
        val $methodDescriptorValName: _root_.io.grpc.MethodDescriptor[$reqElemType, $respElemType] =
          $methodDescriptorDefName
      """

      val methodDescriptorObj: ModuleDef = q"""
        object $methodDescriptorName {
          $methodDescriptorDef
          $methodDescriptorVal
        }
      """

      private def clientCallMethodFor(clientMethodName: String) =
        q"$clientCallsImpl.${TermName(clientMethodName)}[$F, $reqElemType, $respElemType](input, $methodDescriptorName.$methodDescriptorValName, channel, options)"

      val clientDef: Tree = streamingType match {
        case Some(RequestStreaming) =>
          q"""
          def $name(input: $reqType): $wrappedRespType =
            ${clientCallMethodFor("clientStreaming")}
          """
        case Some(ResponseStreaming) =>
          q"""
          def $name(input: $reqType): $wrappedRespType =
            ${clientCallMethodFor("serverStreaming")}
          """
        case Some(BidirectionalStreaming) =>
          q"""
          def $name(input: $reqType): $wrappedRespType =
            ${clientCallMethodFor("bidiStreaming")}
          """
        case None =>
          q"""
          def $name(input: $reqType): $wrappedRespType =
            ${clientCallMethodFor("unary")}
          """
      }

      // Kleisli[F, Span[F], Resp]
      private val kleisliFSpanFResp = kleisliFSpanFB(respElemType)

      val tracingClientDef: Tree = (streamingType, prevalentStreamingTarget) match {
        case (None, _) =>
          // def foo(input: Req): Kleisli[F, Span[F], Resp]
          q"""
          def $name(input: $reqType): $kleisliFSpanFResp =
            ${clientCallMethodFor("tracingUnary")}
          """
        case (Some(RequestStreaming), _: Fs2StreamTpe) =>
          // def foo(input: Stream[Kleisli[F, Span[F], *], Req]): Kleisli[F, Span[F], Resp]
          q"""
          def $name(input: _root_.fs2.Stream[$kleisliFSpanF, $reqElemType]): $kleisliFSpanFResp =
            ${clientCallMethodFor("tracingClientStreaming")}
          """
        case (Some(RequestStreaming), _: MonixObservableTpe) =>
          // def foo(input: Observable[Req]): Kleisli[F, Span[F], Resp]
          q"""
          def $name(input: $reqType): $kleisliFSpanFResp =
            ${clientCallMethodFor("tracingClientStreaming")}
          """
        case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
          // def foo(input: Req): Kleisli[F, Span[F], Stream[Kleisli[F, Span[F], *], Resp]]
          val returnType = kleisliFSpanFB(tq"_root_.fs2.Stream[$kleisliFSpanF, $respElemType]")
          q"""
          def $name(input: $reqType): $returnType =
            ${clientCallMethodFor("tracingServerStreaming")}
          """
        case (Some(ResponseStreaming), _: MonixObservableTpe) =>
          // def foo(input: Req): Kleisli[F, Span[F], Observable[Resp]]
          q"""
          def $name(input: $reqType): ${kleisliFSpanFB(unwrappedRespType)} =
            ${clientCallMethodFor("tracingServerStreaming")}
          """
        case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
          // def foo(input: Stream[Kleisli[F, Span[F], *], Req]): Stream[Kleisli[F, Span[F], *], Resp]
          val returnType = kleisliFSpanFB(tq"_root_.fs2.Stream[$kleisliFSpanF, $respElemType]")
          q"""
          def $name(input: _root_.fs2.Stream[$kleisliFSpanF, $reqElemType]): $returnType =
            ${clientCallMethodFor("tracingBidiStreaming")}
          """
        case (Some(BidirectionalStreaming), _: MonixObservableTpe) =>
          // def foo(input: Observable[Req]): Kleisli[F, Span[F], Observable[Resp]]
          q"""
          def $name(input: $reqType): ${kleisliFSpanFB(unwrappedRespType)} =
            ${clientCallMethodFor("tracingBidiStreaming")}
          """
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"Unable to define a tracing client method for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service $serviceName"
          )
      }

      val serverCallHandler: Tree = (streamingType, prevalentStreamingTarget) match {
        case (Some(RequestStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[$F, $reqElemType, $respElemType](
            { (req: _root_.fs2.Stream[$F, $reqElemType], $anonymousParam) => algebra.$name(req) },
            $compressionTypeTree
          )
          """
        case (Some(RequestStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.clientStreaming[$F, $reqElemType, $respElemType](
            algebra.$name,
            $compressionTypeTree
          )
          """

        case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[$F, $reqElemType, $respElemType](
            { (req: $reqType, $anonymousParam) => algebra.$name(req) },
            $compressionTypeTree
          )
          """
        case (Some(ResponseStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.serverStreaming[$F, $reqElemType, $respElemType](
            algebra.$name,
            $compressionTypeTree
          )
          """

        case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[$F, $reqElemType, $respElemType](
            { (req: _root_.fs2.Stream[$F, $reqElemType], $anonymousParam) => algebra.$name(req) },
            $compressionTypeTree
          )
          """
        case (Some(BidirectionalStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.bidiStreaming[$F, $reqElemType, $respElemType](
            algebra.$name,
            $compressionTypeTree
          )
          """

        case (None, _) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.handlers.unary[$F, $reqElemType, $respElemType](
            algebra.$name,
            $compressionTypeTree
          )
          """
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"Unable to define a handler for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service $serviceName"
          )
      }

      val descriptorAndHandler: Tree =
        q"($methodDescriptorName.$methodDescriptorValName, $serverCallHandler)"

      val tracingServerCallHandler: Tree = (streamingType, prevalentStreamingTarget) match {
        case (Some(RequestStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.tracingClientStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (Some(RequestStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.tracingClientStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.tracingServerStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (Some(ResponseStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.tracingServerStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.tracingBidiStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (Some(BidirectionalStreaming), _: MonixObservableTpe) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.monix.handlers.tracingBidiStreaming(
            algebra.$name _,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case (None, _) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.handlers.tracingUnary[$F, $reqElemType, $respElemType](
            algebra.$name,
            $methodDescriptorName.$methodDescriptorValName,
            entrypoint,
            $compressionTypeTree
          )
          """
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"Unable to define a tracing handler for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service $serviceName"
          )
      }

      val descriptorAndTracingHandler: Tree = {
        q"($methodDescriptorName.$methodDescriptorValName, $tracingServerCallHandler)"
      }

    }

    case class HttpOperation(operation: Operation) {

      import operation._

      val uri = name.toString

      val method: TermName = request match {
        case _: EmptyTpe => TermName("GET")
        case _           => TermName("POST")
      }

      val requestTypology: Tree = request match {
        case _: UnaryTpe =>
          q"val request = _root_.org.http4s.Request[F](_root_.org.http4s.Method.$method, uri / ${uri
            .replace("\"", "")}).withEntity(req.asJson)"
        case _: Fs2StreamTpe =>
          q"val request = _root_.org.http4s.Request[F](_root_.org.http4s.Method.$method, uri / ${uri
            .replace("\"", "")}).withEntity(req.map(_.asJson))"
        case _ =>
          q"val request = _root_.org.http4s.Request[F](_root_.org.http4s.Method.$method, uri / ${uri
            .replace("\"", "")})"
      }

      val executionClient: Tree = response match {
        case _: Fs2StreamTpe =>
          q"_root_.cats.Applicative[F].pure(client.stream(request).flatMap(_.asStream[${response.messageType}]))"
        case _ =>
          q"""client.expectOr[${response.messageType}](request)(handleResponseError)(_root_.org.http4s.circe.jsonOf[F, ${response.messageType}])"""
      }

      def toRequestTree: Tree = request match {
        case _: EmptyTpe =>
          q"""def $name(client: _root_.org.http4s.client.Client[F])(
               implicit responseDecoder: _root_.io.circe.Decoder[${response.messageType}]): ${response.originalType} = {
                 $requestTypology
                 $executionClient
               }"""
        case _ =>
          q"""def $name(req: ${request.originalType})(client: _root_.org.http4s.client.Client[F])(
               implicit requestEncoder: _root_.io.circe.Encoder[${request.messageType}],
               responseDecoder: _root_.io.circe.Decoder[${response.messageType}]
            ): ${response.originalType} = {
              $requestTypology
              $executionClient
            }"""
      }

      val routeTypology: Tree = (request, response) match {
        // Stream -> Stream
        case (_: Fs2StreamTpe, _: Fs2StreamTpe) =>
          q"""val requests = msg.asStream[${operation.request.messageType}]
              for {
                respStream <- handler.${operation.name}(requests)
                responses  <- _root_.org.http4s.Status.Ok.apply(respStream.asJsonEither)
              } yield responses"""

        // Stream -> Empty
        case (_: Fs2StreamTpe, _: EmptyTpe) =>
          q"""val requests = msg.asStream[${operation.request.messageType}]
              _root_.org.http4s.Status.Ok.apply(handler.${operation.name}(requests).as(()))"""

        // Stream -> Unary
        case (_: Fs2StreamTpe, _: UnaryTpe) =>
          q"""val requests = msg.asStream[${operation.request.messageType}]
              _root_.org.http4s.Status.Ok.apply(handler.${operation.name}(requests).map(_.asJson))"""

        // Empty -> Stream
        case (_: EmptyTpe, _: Fs2StreamTpe) =>
          q"""for {
                respStream <- handler.${operation.name}(_root_.higherkindness.mu.rpc.protocol.Empty)
                responses  <- _root_.org.http4s.Status.Ok.apply(respStream.asJsonEither)
              } yield responses"""

        // Empty -> Empty
        case (_: EmptyTpe, _: EmptyTpe) =>
          q"""_root_.org.http4s.Status.Ok.apply(handler.${operation.name}(_root_.higherkindness.mu.rpc.protocol.Empty).as(()))"""

        // Empty -> Unary
        case (_: EmptyTpe, _: UnaryTpe) =>
          q"""_root_.org.http4s.Status.Ok.apply(handler.${operation.name}(_root_.higherkindness.mu.rpc.protocol.Empty).map(_.asJson))"""

        // Unary -> Stream
        case (_: UnaryTpe, _: Fs2StreamTpe) =>
          q"""for {
              request    <- msg.as[${operation.request.messageType}]
              respStream <- handler.${operation.name}(request)
              responses  <- _root_.org.http4s.Status.Ok.apply(respStream.asJsonEither)
            } yield responses"""

        // Unary -> Empty
        case (_: UnaryTpe, _: EmptyTpe) =>
          q"""for {
              request  <- msg.as[${operation.request.messageType}]
              response <- _root_.org.http4s.Status.Ok.apply(handler.${operation.name}(request).as(())).adaptErrors
            } yield response"""

        // Unary -> Unary
        case _ =>
          q"""for {
              request  <- msg.as[${operation.request.messageType}]
              response <- _root_.org.http4s.Status.Ok.apply(handler.${operation.name}(request).map(_.asJson)).adaptErrors
            } yield response"""
      }

      val getPattern =
        pq"_root_.org.http4s.Method.GET -> _root_.org.http4s.dsl.impl.Root / ${operation.name.toString}"
      val postPattern =
        pq"msg @ _root_.org.http4s.Method.POST -> _root_.org.http4s.dsl.impl.Root / ${operation.name.toString}"

      def toRouteTree: Tree = request match {
        case _: EmptyTpe => cq"$getPattern => $routeTypology"
        case _           => cq"$postPattern => $routeTypology"
      }

    }

    val operations: List[HttpOperation] = for {
      d <- rpcDefs.collect { case x if findAnnotation(x.mods, "http").isDefined => x }
      // TODO not sure what the following line is doing, as the result is not used. Is it needed?
      _      <- findAnnotation(d.mods, "http").collect({ case Apply(_, args) => args }).toList
      params <- d.vparamss
      _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
      p <- params.headOption.toList
      reqType  = TypeTypology(p.tpt, false, F)
      respType = TypeTypology(d.tpt, true, F)
      op       = Operation(d.name, reqType, respType)
      _ = if (op.isMonixObservable)
        sys.error(
          "Monix.Observable is not compatible with streaming services. Please consider using Fs2.Stream instead."
        )
    } yield HttpOperation(op)

    val streamConstraints: List[Tree] = List(q"F: _root_.cats.effect.Sync[$F]")

    val httpRequests = operations.map(_.toRequestTree)

    val HttpClient      = TypeName("HttpClient")
    val httpClientClass = q"""
        class $HttpClient[$F_](uri: _root_.org.http4s.Uri)(implicit ..$streamConstraints) {
          ..$httpRequests
      }"""

    val httpClient = q"""
        def httpClient[$F_](uri: _root_.org.http4s.Uri)
          (implicit ..$streamConstraints): $HttpClient[$F] = {
          new $HttpClient[$F](uri / ${serviceDef.name.toString})
      }"""

    val httpImports: List[Tree] = List(
      q"import _root_.higherkindness.mu.http.implicits._",
      q"import _root_.cats.syntax.flatMap._",
      q"import _root_.cats.syntax.functor._",
      q"import _root_.org.http4s.circe._",
      q"import _root_.io.circe.syntax._"
    )

    val httpRoutesCases: Seq[Tree] = operations.map(_.toRouteTree)

    val routesPF: Tree = q"{ case ..$httpRoutesCases }"

    val requestTypes: Set[String] =
      operations
        .filterNot(_.operation.request.isEmpty)
        .map(_.operation.request.messageType.toString)
        .toSet

    val responseTypes: Set[String] =
      operations
        .filterNot(_.operation.response.isEmpty)
        .map(_.operation.response.messageType.toString)
        .toSet

    val requestDecoders =
      requestTypes.map(n =>
        q"""implicit private val ${TermName("entityDecoder" + n)}:_root_.org.http4s.EntityDecoder[F, ${TypeName(
          n
        )}] = jsonOf[F, ${TypeName(n)}]"""
      )

    val HttpRestService: TypeName = TypeName(serviceDef.name.toString + "RestService")

    val arguments: List[Tree] = List(q"handler: ${serviceDef.name}[F]") ++
      requestTypes.map(n =>
        q"${TermName("decoder" + n)}: _root_.io.circe.Decoder[${TypeName(n)}]"
      ) ++
      responseTypes.map(n =>
        q"${TermName("encoder" + n)}: _root_.io.circe.Encoder[${TypeName(n)}]"
      ) ++
      streamConstraints

    val httpRestServiceClass: Tree = q"""
        class $HttpRestService[$F_](implicit ..$arguments) extends _root_.org.http4s.dsl.Http4sDsl[F] {
         ..$requestDecoders
         def service = _root_.org.http4s.HttpRoutes.of[F]{$routesPF}
      }"""

    val httpService = q"""
        def route[$F_](implicit ..$arguments): _root_.higherkindness.mu.http.protocol.RouteMap[F] = {
          _root_.higherkindness.mu.http.protocol.RouteMap[F](${serviceDef.name.toString}, new $HttpRestService[$F].service)
      }"""

    val http =
      if (httpRequests.isEmpty) Nil
      else
        httpImports ++ List(httpClientClass, httpClient, httpRestServiceClass, httpService)

  }

}

// $COVERAGE-ON$
