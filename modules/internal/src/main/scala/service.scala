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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import freestyle.free.internal.ScalametaUtil
import freestyle.rpc.protocol._

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._
import _root_.io.grpc.MethodDescriptor.Marshaller

// $COVERAGE-OFF$
object serviceImpl {

  import errors._

  def service(defn: Any): Stat = {
    defn match {
      case cls: Trait =>
        serviceExtras(cls.name, cls)
      case cls: Class if ScalametaUtil.isAbstract(cls) =>
        serviceExtras(cls.name, cls)
      case _ =>
        abort(s"$invalid. $abstractOnly")
    }
  }

  def serviceExtras(name: Type.Name, alg: Defn): Term.Block = {
    import utils._
    val serviceAlg = ServiceAlg(alg)
    Term.Block(Seq(alg) ++ Seq(mkCompanion(name, enrich(serviceAlg, serviceAlg.innerImports))))
  }

  def enrich(serviceAlg: ServiceAlg, companion: Object): Object = companion match {
    case q"..$mods object $ename extends $template" =>
      template match {
        case template"{ ..$earlyInit } with ..$inits { $self => ..$stats }" =>
          val enrichedTemplate =
            template"{ ..$earlyInit } with ..$inits { $self => ..${enrich(serviceAlg, stats)} }"
          q"..$mods object $ename extends $enrichedTemplate"
      }
  }

  def enrich(serviceAlg: RPCService, members: Seq[Stat]): Seq[Stat] =
    members ++
      serviceAlg.methodDescriptors ++
      Seq(serviceAlg.serviceBindings, serviceAlg.client, serviceAlg.clientInstance)
}

trait RPCService {
  import utils._

  def defn: Defn

  def typeParam: Type.Param

  val (algName, template) = defn match {
    case c: Class => (c.name, c.templ)
    case t: Trait => (t.name, t.templ)
  }

  val allServiceStats: List[Stat] = template.stats.toList.flatten

  val rpcMethods: List[Stat] = allServiceStats.filter { s =>
    s match {
      case q"import ..$_" => false
      case _              => true
    }
  }

  val innerImports: List[Stat] = (allServiceStats.toSet -- rpcMethods).toList

  val requests: List[RPCRequest] = buildRequests(algName, typeParam, rpcMethods)

  val methodDescriptors: Seq[Defn.Val] = requests.map(_.methodDescriptor)

  val serviceBindings: Defn.Def = {
    val args: Seq[Term.Tuple] = requests.map(_.call)
    q"""
       def bindService[F[_]](implicit algebra: $algName[F], EFF: _root_.cats.effect.Effect[F], S: _root_.monix.execution.Scheduler): _root_.io.grpc.ServerServiceDefinition =
           new _root_.freestyle.rpc.internal.service.GRPCServiceDefBuilder(${Lit.String(
      algName.value)}, ..$args).apply
     """
  }

  val clientName: Type.Name = Type.Name("Client")

  val wartSuppress =
    mod"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.DefaultArguments"))"""

  val client: Class = {
    val clientDefs: Seq[Defn.Def] = requests.map(_.clientDef)
    q"""
       $wartSuppress
       class $clientName[F[_]: _root_.cats.effect.Effect](channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)
          (implicit S: _root_.monix.execution.Scheduler)
          extends _root_.io.grpc.stub.AbstractStub[$clientName[F]](channel, options) {

          override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $clientName[F] = {
              new ${clientName.ctorRef(Ctor.Name(clientName.value))}[F](channel, options)
          }

          ..$clientDefs

       }
     """
  }

  val clientInstance =
    q"""
       $wartSuppress
       def client[F[_]: _root_.cats.effect.Effect](
         channelFor: _root_.freestyle.rpc.client.ManagedChannelFor,
         channelConfigList: List[_root_.freestyle.rpc.client.ManagedChannelConfig] = List(
           _root_.freestyle.rpc.client.UsePlaintext(true)),
           options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(
         implicit S: _root_.monix.execution.Scheduler): $clientName[F] = {
         val managedChannelInterpreter =
           new _root_.freestyle.rpc.client.ManagedChannelInterpreter[F](channelFor, channelConfigList)
         new ${clientName.ctorRef(Ctor.Name(clientName.value))}[F](managedChannelInterpreter.build(channelFor, channelConfigList), options)
       }
     """
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
    streamingType: Option[StreamingType],
    streamingImpl: Option[StreamingImpl],
    requestType: Type,
    responseType: Type) {

  val wartSuppress =
    mod"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.Null","org.wartremover.warts.ExplicitImplicitTypes"))"""

  val descriptorName: Term.Name = name.copy(value = name.value + "MethodDescriptor")

  val encodersImport: Import = serialization match {
    case Protobuf =>
      q"import _root_.freestyle.rpc.internal.encoders.pbd._"
    case Avro =>
      q"import _root_.freestyle.rpc.internal.encoders.avro._"
  }

  def methodDescriptor =
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

  def streamingImplType(tp: Type): Type = streamingImpl match {
    case Some(Fs2Stream)       => targ"_root_.fs2.Stream[F, $tp]"
    case Some(MonixObservable) => targ"_root_.monix.reactive.Observable[$tp]"
    case None                  => targ"Unit"
  }

  def clientCallsType(methodName: Term.Name): Term.Apply = streamingImpl match {
    case Some(Fs2Stream) =>
      q"_root_.freestyle.rpc.internal.client.fs2Calls.$methodName(input, $descriptorName, channel, options)"
    case Some(MonixObservable) =>
      q"_root_.freestyle.rpc.internal.client.monixCalls.$methodName(input, $descriptorName, channel, options)"
    case _ =>
      q"_root_.freestyle.rpc.internal.client.monixCalls.$methodName(input, $descriptorName, channel, options)"
  }

  val clientDef: Defn.Def = streamingType match {
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

  def serverCallsType(methodName: Term.Name): Term.Apply = streamingImpl match {
    case Some(Fs2Stream) =>
      q"_root_.freestyle.rpc.internal.server.fs2Calls.$methodName(algebra.$name)"
    case Some(MonixObservable) =>
      q"_root_.freestyle.rpc.internal.server.monixCalls.$methodName(algebra.$name)"
    case _ =>
      q"_root_.freestyle.rpc.internal.server.monixCalls.$methodName(algebra.$name)"
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

  def mkCompanion(name: Type.Name, stats: Seq[Stat]): Object = {
    val prot = q"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array(
               "org.wartremover.warts.Any",
               "org.wartremover.warts.NonUnitStatements",
               "org.wartremover.warts.StringPlusAny",
               "org.wartremover.warts.Throw"))
               object X {}"""

    prot.copy(
      name = Term.Name(name.value),
      templ = prot.templ.copy(
        stats = Some(stats)
      ))
  }

  def paramTpe(param: Term.Param): Type = {
    val Term.Param(_, paramname, Some(ptpe), _) = param
    val targ"${tpe: Type}"                      = ptpe
    tpe
  }

  // format: OFF
  def buildRequests(algName: Type.Name, typeParam: Type.Param, stats: List[Stat]): List[RPCRequest] = stats.collect {
    case q"@rpc($s) @stream[ResponseStreaming.type] def $name[..$tparams]($request): Stream[$resTypeParam, $response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(ResponseStreaming), Some(Fs2Stream), paramTpe(request), response)

    case q"@rpc($s) @stream[ResponseStreaming.type] def $name[..$tparams]($request): Observable[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(ResponseStreaming), Some(MonixObservable), paramTpe(request), response)

    case q"@rpc($s) @stream[RequestStreaming.type] def $name[..$tparams]($paranName: Stream[$reqTypeParam, $request]): $typeParam[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(RequestStreaming), Some(Fs2Stream), request, response)

    case q"@rpc($s) @stream[RequestStreaming.type] def $name[..$tparams]($paranName: Observable[$request]): $typeParam[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(RequestStreaming), Some(MonixObservable), request, response)

    case q"@rpc($s) @stream[BidirectionalStreaming.type] def $name[..$tparams]($paranName: Stream[$reqTypeParam, $request]): Stream[$resTypeParam, $response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(BidirectionalStreaming), Some(Fs2Stream), request, response)

    case q"@rpc($s) @stream[BidirectionalStreaming.type] def $name[..$tparams]($paranName: Observable[$request]): Observable[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(BidirectionalStreaming), Some(MonixObservable), request, response)

    case q"@rpc($s) def $name[..$tparams]($request): $typeParam[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), None, None, paramTpe(request), response)
  }
  // format: ON

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

}

private[internal] object errors {
  val invalid = "Invalid use of `@service`"
  val abstractOnly =
    "`@service` can only annotate a trait or abstract class already annotated with @tagless"
}

object encoders {

  object pbd {

    import pbdirect._

    implicit def defaultDirectPBMarshallers[A: PBWriter: PBReader]: Marshaller[A] =
      new Marshaller[A] {

        override def parse(stream: InputStream): A =
          Iterator.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray.pbTo[A]

        override def stream(value: A): InputStream = new ByteArrayInputStream(value.toPB)

      }
  }

  object avro {

    import com.sksamuel.avro4s._

    implicit val emptyMarshallers: Marshaller[Empty.type] = new Marshaller[Empty.type] {
      override def parse(stream: InputStream) = Empty
      override def stream(value: Empty.type)  = new ByteArrayInputStream(Array.empty)
    }

    implicit def avroMarshallers[A: SchemaFor: FromRecord: ToRecord]: Marshaller[A] =
      new Marshaller[A] {

        override def parse(stream: InputStream): A = {
          val bytes: Array[Byte] =
            Iterator.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
          val in: ByteArrayInputStream        = new ByteArrayInputStream(bytes)
          val input: AvroBinaryInputStream[A] = AvroInputStream.binary[A](in)
          input.iterator().toList.head
        }

        override def stream(value: A): InputStream = {
          val baos: ByteArrayOutputStream       = new ByteArrayOutputStream()
          val output: AvroBinaryOutputStream[A] = AvroOutputStream.binary[A](baos)
          output.write(value)
          output.close()

          new ByteArrayInputStream(baos.toByteArray)
        }

      }
  }

}

// $COVERAGE-ON$
