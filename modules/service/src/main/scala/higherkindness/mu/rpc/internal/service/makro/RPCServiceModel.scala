/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.internal.service.makro

import higherkindness.mu.rpc.protocol._
import scala.reflect.macros.blackbox.Context

// $COVERAGE-OFF$
class RPCServiceModel[C <: Context](val c: C) {
  import c.universe._

  val wartSuppression = new WartSuppression[c.type](c)
  import wartSuppression._

  val treeHelpers = new TreeHelpers[c.type](c)
  import treeHelpers._

  val operationModels = new OperationModels[c.type](c)
  import operationModels._
  import operationModels.typeAnalysis._

  class RPCService(serviceDef: ClassDef) {
    val serviceName: TypeName = serviceDef.name

    require(
      serviceDef.tparams.nonEmpty && serviceDef.tparams.length <= 2,
      s"@service-annotated class $serviceName must have a one or two type parameters"
    )

    val F_ : TypeDef = serviceDef.tparams.head
    val F: TypeName  = F_.name

    val MC_ : Option[TypeDef] = serviceDef.tparams.lift(1)
    val MC: Option[TypeName]  = MC_.map(_.name)

    require(
      F_.tparams.length == 1,
      s"@service-annotated class $serviceName's type parameter must be higher-kinded"
    )

    require(
      MC_.forall(_.tparams.isEmpty),
      s"@service-annotated class $serviceName's context type parameter must not be higher-kinded"
    )

    // Type lambda for Kleisli[F, MC, *]. `Span[F]` by default
    private val kleisliFContext: SelectFromTypeTree =
      MC.fold(tq"({ type T[α] = _root_.cats.data.Kleisli[$F, _root_.natchez.Span[$F], α] })#T") { context =>
        tq"({ type T[α] = _root_.cats.data.Kleisli[$F, $context, α] })#T"
      }


    private val defs: List[Tree] = serviceDef.impl.body

    private val (rpcDefs, nonRpcDefs) = defs.collect { case d: DefDef =>
      d
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

    private val rpcRequests: List[RPCMethod] = for {
      d      <- rpcDefs
      params <- d.vparamss
      _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
      p <- params.headOption.toList
    } yield {
      val requestType  = TypeTypology(p.tpt, false, F)
      val responseType = TypeTypology(d.tpt, true, F)
      RPCMethod(
        EnclosingService(
          serviceName,
          fullServiceName,
          compressionType,
          methodNameStyle,
          F,
          MC,
          kleisliFContext
        ),
        Operation(d.name, requestType, responseType)
      )
    }

    val imports: List[Tree] = defs.collect { case imp: Import =>
      imp
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

    val ceImplicit: Tree        = q"CE: _root_.cats.effect.Async[$F]"
    val schedulerImplicit: Tree = q"S: _root_.monix.execution.Scheduler"

    val bindImplicits: List[Tree] = ceImplicit :: q"algebra: $serviceName[$F]" :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val classImplicits: List[Tree] = ceImplicit :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val bindService: DefDef = q"""
      def bindService[$F_](implicit ..$bindImplicits): _root_.cats.effect.Resource[$F, _root_.io.grpc.ServerServiceDefinition] =
        _root_.cats.effect.std.Dispatcher.apply[$F](CE).evalMap { disp =>
          _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[$F](
            ${lit(fullServiceName)},
            ..$serverCallDescriptorsAndHandlers
          )
        }
      """

    private val serverCallDescriptorsAndTracingHandlers: List[Tree] =
      rpcRequests.map(_.descriptorAndContextHandler)

    val contextAlgebra = q"algebra: $serviceName[$kleisliFContext]"
    val bindContextServiceImplicits: List[Tree] = ceImplicit :: contextAlgebra :: rpcRequests
      .find(_.operation.isMonixObservable)
      .map(_ => schedulerImplicit)
      .toList

    val bindContextService: DefDef = q"""
      def bindContextService[$F_](implicit ..$bindContextServiceImplicits): _root_.cats.effect.Resource[$F, _root_.io.grpc.ServerServiceDefinition] =
        _root_.cats.effect.std.Dispatcher.apply[$F](CE).evalMap { disp =>
          _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[$F](
            ${lit(fullServiceName)},
            ..$serverCallDescriptorsAndTracingHandlers
          )
        }
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
        ).evalMap(ch =>
          CE.delay(new $Client[$F](ch, options))
        )
      """.suppressWarts("DefaultArguments")

    val unsafeClient: DefDef =
      q"""
      def unsafeClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        disp: _root_.cats.effect.std.Dispatcher[$F],
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$F] = {
        val managedChannelInterpreter =
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).unsafeBuild(disp)
        new $Client[$F](managedChannelInterpreter, options)
      }""".suppressWarts("DefaultArguments")

    val unsafeClientFromChannel: DefDef =
      q"""
      def unsafeClientFromChannel[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$F] = new $Client[$F](channel, options)
      """.suppressWarts("DefaultArguments")

    private val contextClientCallMethods: List[Tree] = rpcRequests.map(_.contextClientDef)
    private val ContextClient                        = TypeName("ContextClient")
    val contextClientClass: ClassDef =
      q"""
      class $ContextClient[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits) extends _root_.io.grpc.stub.AbstractStub[$ContextClient[$F]](channel, options) with $serviceName[$kleisliFContext] {
        override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $ContextClient[$F] =
            new $ContextClient[$F](channel, options)

        ..$contextClientCallMethods
        ..$nonRpcDefs
      }""".suppressWarts("DefaultArguments")

    val contextClient: DefDef =
      q"""
      def contextClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[F, $serviceName[$kleisliFContext]] =
        _root_.cats.effect.Resource.make(
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).build
        )(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[F, $serviceName[$kleisliFContext]](
            CE.delay(new $ContextClient[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val contextClientFromChannel: DefDef =
      q"""
      def contextClientFromChannel[$F_](
        channel: $F[_root_.io.grpc.ManagedChannel],
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): _root_.cats.effect.Resource[$F, $serviceName[$kleisliFContext]] =
        _root_.cats.effect.Resource.make(channel)(channel =>
          CE.void(CE.delay(channel.shutdown()))
        ).flatMap(ch =>
          _root_.cats.effect.Resource.make[$F, $serviceName[$kleisliFContext]](
            CE.delay(new $ContextClient[$F](ch, options))
          )($anonymousParam =>
            CE.unit
          )
        )
      """.suppressWarts("DefaultArguments")

    val unsafeContextClient: DefDef =
      q"""
      def unsafeContextClient[$F_](
        channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
        channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] =
          List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
        disp: _root_.cats.effect.std.Dispatcher[$F],
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$kleisliFContext] = {
        val managedChannelInterpreter =
          new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[$F](channelFor, channelConfigList).unsafeBuild(disp)
        new $ContextClient[$F](managedChannelInterpreter, options)
      }""".suppressWarts("DefaultArguments")

    val unsafeContextClientFromChannel: DefDef =
      q"""
      def unsafeContextClientFromChannel[$F_](
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
      )(implicit ..$classImplicits): $serviceName[$kleisliFContext] = new $ContextClient[$F](channel, options)
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

  }

}
// $COVERAGE-ON$
