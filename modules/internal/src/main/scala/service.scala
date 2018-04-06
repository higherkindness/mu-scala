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

import cats.instances.list._
import cats.syntax.foldable._

import freestyle.free.internal.ScalametaUtil
import freestyle.rpc.protocol._

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._

// $COVERAGE-OFF$
object serviceImpl {

  import errors._

  def service(defn: Any): Stat = {
    defn match {
      case cls: Trait =>
        serviceExtras(cls.name, cls)
      case cls: Class if ScalametaUtil.isAbstract(cls) =>
        serviceExtras(cls.name, cls)
      case Term.Block(Seq(cls: Trait, companion: Object)) =>
        serviceExtras(cls.name, cls, Some(companion))
      case Term.Block(Seq(cls: Class, companion: Object)) if ScalametaUtil.isAbstract(cls) =>
        serviceExtras(cls.name, cls, Some(companion))
      case _ =>
        abort(s"$invalid. $abstractOnly")
    }
  }

  private[this] def serviceExtras(
      name: Type.Name,
      alg: Defn,
      companion: Option[Object] = None): Term.Block = {
    import utils._
    val serviceAlg = ServiceAlg(alg)
    val enrichedCompanion = {
      val stats = enrich(serviceAlg, serviceAlg.innerImports)
      companion.fold(mkCompanion(name, stats))(enrichCompanion(_, stats))
    }
    Term.Block(alg :: enrichedCompanion :: Nil)
  }

  private[this] def enrich(serviceAlg: RPCService, members: Seq[Stat]): Seq[Stat] =
    members ++
      serviceAlg.methodDescriptors ++
      Seq(serviceAlg.serviceBindings, serviceAlg.clientClass) ++ serviceAlg.clientInstance
}

trait RPCService {
  import utils._

  def defn: Defn

  def typeParam: Type.Param

  private[this] val (algName, template) = defn match {
    case c: Class => (c.name, c.templ)
    case t: Trait => (t.name, t.templ)
  }

  private[this] val (imports, noImports) = {
    def isImport(stat: Stat): Boolean = stat match {
      case q"import ..$_" => true
      case _              => false
    }

    val allServiceStats: List[Stat] = template.stats.toList.flatten
    allServiceStats.partition(isImport)
  }

  val innerImports: List[Stat] = imports

  val (noRequests, requests): (List[Stat], List[RPCRequest]) =
    buildRequests(algName, typeParam, noImports)

  val methodDescriptors: Seq[Defn.Val] = requests.map(_.methodDescriptor)

  val serviceBindings: Defn.Def = {
    val args: Seq[Term.Tuple] = requests.map(_.call)
    q"""
       def bindService[F[_]](implicit
         F: _root_.cats.effect.Effect[F],
         algebra: $algName[F],
         S: _root_.monix.execution.Scheduler
       ): _root_.io.grpc.ServerServiceDefinition =
           new _root_.freestyle.rpc.internal.service.GRPCServiceDefBuilder(${Lit.String(
      algName.value)}, ..$args).apply
     """
  }

  private[this] val clientName: Type.Name = Type.Name("Client")
  private[this] val clientCtor            = clientName.ctorRef(Ctor.Name(clientName.value))

  private[this] val wartSuppress = surpressWarts("DefaultArguments")

  val clientClass: Class = {
    val clientDefs: Seq[Defn.Def] = requests.map(_.clientDef)
    q"""
       $wartSuppress
       class $clientName[F[_]](
         channel: _root_.io.grpc.Channel,
         options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
       )(implicit
         F: _root_.cats.effect.Effect[F],
         S: _root_.monix.execution.Scheduler
       ) extends _root_.io.grpc.stub.AbstractStub[$clientName[F]](channel, options) {

          override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $clientName[F] =
              new $clientCtor[F](channel, options)

          ..$clientDefs

          ..$noRequests
       }
     """
  }

  val clientInstance = Seq(
    q"""
       $wartSuppress
       def client[F[_]](
         channelFor: _root_.freestyle.rpc.ChannelFor,
         channelConfigList: List[_root_.freestyle.rpc.client.ManagedChannelConfig] = List(
           _root_.freestyle.rpc.client.UsePlaintext()),
         options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
       )(implicit
         F: _root_.cats.effect.Effect[F],
         S: _root_.monix.execution.Scheduler
       ): $clientName[F] = {
         val managedChannelInterpreter =
           new _root_.freestyle.rpc.client.ManagedChannelInterpreter[F](channelFor, channelConfigList)
         new $clientCtor[F](managedChannelInterpreter.build(channelFor, channelConfigList), options)
       }""",
    q"""
       $wartSuppress
       def clientFromChannel[F[_]](
         channel: _root_.io.grpc.Channel,
         options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
       )(implicit
         F: _root_.cats.effect.Effect[F],
         S: _root_.monix.execution.Scheduler
        ): $clientName[F] = new $clientCtor[F](channel, options)
     """
  )
}

case class ServiceAlg(defn: Defn) extends RPCService {
  val typeParam: Type.Param = (defn match {
    case c: Class => c.tparams.headOption
    case t: Trait => t.tparams.headOption
  }) getOrElse abort("Type parameter must be specified")
}

private[internal] case class RPCRequest(
    algName: Type.Name,
    name: Term.Name,
    serialization: SerializationType,
    compression: CompressionType,
    streamingType: Option[StreamingType],
    streamingImpl: Option[StreamingImpl],
    requestType: Type,
    responseType: Type) {

  private[this] val descriptorName: Term.Name = name.copy(value = name.value + "MethodDescriptor")

  def methodDescriptor = {
    val wartSuppress = utils.surpressWarts("Null", "ExplicitImplicitTypes")
    val encodersImport: Import = serialization match {
      case Protobuf =>
        q"import _root_.freestyle.rpc.internal.encoders.pbd._"
      case Avro =>
        q"import _root_.freestyle.rpc.internal.encoders.avro._"
    }

    q"""
       $wartSuppress
       val ${Pat.Var.Term(descriptorName)}: _root_.io.grpc.MethodDescriptor[$requestType, $responseType] = {

         $encodersImport

         _root_.io.grpc.MethodDescriptor
           .newBuilder(
             implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$requestType]],
             implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$responseType]])
           .setType(${utils.methodType(streamingType)})
           .setFullMethodName(
             _root_.io.grpc.MethodDescriptor.generateFullMethodName(${Lit.String(algName.value)}, ${Lit
      .String(name.value)}))
           .build()
       }
      """
  }

  val clientDef: Defn.Def = {
    def streamingImplType(tp: Type): Type = utils.StreamImpl.resultTypeFor(streamingImpl)(tp)

    def clientCallsType(methodName: Term.Name): Term.Apply = {
      val calls = utils.StreamImpl.clientCalls(streamingImpl.getOrElse(MonixObservable))
      q"$calls.$methodName(input, $descriptorName, channel, options)"
    }

    streamingType match {
      case Some(RequestStreaming) =>
        q"""
           def $name(input: ${streamingImplType(requestType)}): F[$responseType] =
             ${clientCallsType(q"clientStreaming")}
         """
      case Some(ResponseStreaming) =>
        q"""
           def $name(input: $requestType): ${streamingImplType(responseType)} =
             ${clientCallsType(q"serverStreaming")}
         """
      case Some(BidirectionalStreaming) =>
        q"""
           def $name(input: ${streamingImplType(requestType)}): ${streamingImplType(responseType)} =
             ${clientCallsType(q"bidiStreaming")}
         """
      case None =>
        q"""
           def $name(input: $requestType): F[$responseType] =
             ${clientCallsType(q"unary")}
        """
    }
  }

  private[this] def serverCallsType(methodName: Term.Name): Term.Apply = {
    val calls = utils.StreamImpl.serverCalls(streamingImpl.getOrElse(MonixObservable))
    val maybeAlg = compression match {
      case Identity => q"None"
      case Gzip     => q"""Some("gzip")"""
    }
    q"$calls.$methodName(algebra.$name, $maybeAlg)"
  }

  val call: Term.Tuple = {
    val handler = streamingType match {
      case Some(RequestStreaming) =>
        q"_root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(${serverCallsType(q"clientStreamingMethod")})"
      case Some(ResponseStreaming) =>
        q"_root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(${serverCallsType(q"serverStreamingMethod")})"
      case Some(BidirectionalStreaming) =>
        q"_root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(${serverCallsType(q"bidiStreamingMethod")})"
      case None =>
        q"_root_.io.grpc.stub.ServerCalls.asyncUnaryCall(${serverCallsType(q"unaryMethod")})"
    }
    q"($descriptorName, $handler)"
  }
}

private[internal] object utils {

  def surpressWarts(warts: String*): Mod.Annot = {
    val wartsString = warts.toList.map(w => "org.wartremover.warts." + w)
    val argList     = wartsString.map(ws => arg"$ws")

    mod"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array(..$argList))"""
  }

  def mkCompanion(name: Type.Name, stats: Seq[Stat]): Object = {
    val prot = q"object X {}"
    val obj  = prot.copy(name = Term.Name(name.value))
    enrichCompanion(obj, stats)
  }

  def enrichCompanion(companion: Defn.Object, stats: Seq[Stat]): Object = {
    val warts = surpressWarts("Any", "NonUnitStatements", "StringPlusAny", "Throw")
    import companion._
    copy(
      mods = warts +: mods,
      templ = templ.copy(stats = Some(templ.stats.getOrElse(Nil) ++ stats)))
  }

  def paramTpe(param: Term.Param): Type = {
    val Term.Param(_, paramname, Some(ptpe), _) = param
    val targ"${tpe: Type}"                      = ptpe
    tpe
  }

  def buildRequests(
      algName: Type.Name,
      typeParam: Type.Param,
      stats: List[Stat]): (List[Stat], List[RPCRequest]) =
    stats.partitionEither(stat => buildRequest(algName, typeParam, stat).toRight(stat))

  // format: OFF
  def buildRequest(algName: Type.Name, typeParam: Type.Param, stat: Stat): Option[RPCRequest] = Option(stat).collect {
    case q"@rpc(..$s) @stream[ResponseStreaming.type] def $name[..$tparams]($request): ${StreamImpl(impl, response)}" =>
      RPCRequest(algName, name, utils.serializationType(s.head), utils.compressionType(s.lastOption), Some(ResponseStreaming), Some(impl), paramTpe(request), response)

    case q"@rpc(..$s) @stream[RequestStreaming.type] def $name[..$tparams]($paranName: ${Some(StreamImpl(impl, request))}): $typeParam[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s.head), utils.compressionType(s.lastOption), Some(RequestStreaming), Some(impl), request, response)

    case q"@rpc(..$s) @stream[BidirectionalStreaming.type] def $name[..$tparams]($paranName: ${Some(StreamImpl(implReq, request))}): ${StreamImpl(implResp, response)}" if implReq == implResp =>
      RPCRequest(algName, name, utils.serializationType(s.head), utils.compressionType(s.lastOption), Some(BidirectionalStreaming), Some(implReq), request, response)

    case q"@rpc(..$s) def $name[..$tparams]($request): $typeParam[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s.head), utils.compressionType(s.lastOption), None, None, paramTpe(request), response)
  }
  // format: ON

  private[internal] object StreamImpl {
    def unapply(tpe: Type): Option[(StreamingImpl, Type)] =
      tpe match {
        case t"Stream[${_}, $resultTpe]" => Some((Fs2Stream, resultTpe))
        case t"Observable[$resultTpe]"   => Some((MonixObservable, resultTpe))
        case _                           => None
      }

    def resultTypeFor(streamingImpl: Option[StreamingImpl])(tp: Type): Type =
      streamingImpl match {
        case Some(Fs2Stream)       => targ"_root_.fs2.Stream[F, $tp]"
        case Some(MonixObservable) => targ"_root_.monix.reactive.Observable[$tp]"
        case None                  => targ"Unit"
      }

    def clientCalls(streamingImpl: StreamingImpl): Term.Select =
      streamingImpl match {
        case Fs2Stream       => q"_root_.freestyle.rpc.internal.client.fs2Calls"
        case MonixObservable => q"_root_.freestyle.rpc.internal.client.monixCalls"
      }

    def serverCalls(streamingImpl: StreamingImpl): Term.Select =
      streamingImpl match {
        case Fs2Stream       => q"_root_.freestyle.rpc.internal.server.fs2Calls"
        case MonixObservable => q"_root_.freestyle.rpc.internal.server.monixCalls"
      }
  }

  private[internal] def methodType(s: Option[StreamingType]): Term.Select = {
    val suffix = s match {
      case Some(RequestStreaming)       => q"CLIENT_STREAMING"
      case Some(ResponseStreaming)      => q"SERVER_STREAMING"
      case Some(BidirectionalStreaming) => q"BIDI_STREAMING"
      case None                         => q"UNARY"
    }
    q"_root_.io.grpc.MethodDescriptor.MethodType.$suffix"
  }

  private[internal] def serializationType(s: Term.Arg): SerializationType = s match {
    case q"Protobuf" => Protobuf
    case q"Avro"     => Avro
  }

  private[internal] def compressionType(s: Option[Term.Arg]): CompressionType =
    s flatMap {
      case q"Identity" => Some(Identity)
      case q"Gzip"     => Some(Gzip)
      case _           => None
    } getOrElse Identity

}

private[internal] object errors {
  val invalid = "Invalid use of `@service`"
  val abstractOnly =
    "`@service` can only annotate a trait or abstract class"
}

// $COVERAGE-ON$
