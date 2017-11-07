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

package freestyle.rpc
package internal.service

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import freestyle.internal.ScalametaUtil
import freestyle.rpc.protocol._

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._
import _root_.io.grpc.MethodDescriptor.Marshaller

// $COVERAGE-OFF$
object serviceImpl {

  import errors._

  def service(defn: Any): Stat = defn match {
    case Term.Block(Seq(cls: Trait, companion: Object)) =>
      serviceExtras(cls, companion)
    case Term.Block(Seq(cls: Class, companion: Object)) if ScalametaUtil.isAbstract(cls) =>
      serviceExtras(cls, companion)
    case _ =>
      abort(s"$invalid. $abstractOnly")
  }

  def serviceExtras(alg: Defn, companion: Object): Term.Block = {
    val serviceAlg = ServiceAlg(alg)
    Term.Block(Seq(alg, enrich(serviceAlg, companion)))
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

  def enrich(serviceAlg: ServiceAlg, members: Seq[Stat]): Seq[Stat] =
    members ++
      Seq(serviceAlg.commonImports) ++
      serviceAlg.methodDescriptors ++
      Seq(serviceAlg.serviceBindings, serviceAlg.client, serviceAlg.clientInstance)

}

case class ServiceAlg(defn: Defn) {

  val (algName, template) = defn match {
    case c: Class => (c.name, c.templ)
    case t: Trait => (t.name, t.templ)
  }

  private[this] def paramTpe(param: Term.Param): Type = {
    val Term.Param(_, paramname, Some(ptpe), _) = param
    val targ"${tpe: Type}"                      = ptpe
    tpe
  }

  // format: OFF
  val requests: List[RPCRequest] = template.stats.toList.flatten.collect {
    case q"@rpc($s) @stream[ResponseStreaming.type] def $name[..$tparams]($request): FS[Observable[$response]]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(ResponseStreaming), paramTpe(request), response)
    case q"@rpc($s) @stream[RequestStreaming.type] def $name[..$tparams]($paranName: Observable[$request]): FS[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(RequestStreaming), request, response)
    case q"@rpc($s) @stream[BidirectionalStreaming.type] def $name[..$tparams]($paranName: Observable[$request]): FS[Observable[$response]]" =>
      RPCRequest(algName, name, utils.serializationType(s), Some(BidirectionalStreaming), request, response)
    case q"@rpc($s) def $name[..$tparams]($request): FS[$response]" =>
      RPCRequest(algName, name, utils.serializationType(s), None, paramTpe(request), response)
    case e => throw new MatchError("Unmatched rpc method: " + e.toString())
  }
  // format: ON

  val methodDescriptors: Seq[Defn.Val] = requests.map(_.methodDescriptor)

  val commonImports: Import =
    q"import _root_.cats.instances.list._, _root_.cats.instances.option._"

  val serviceBindings: Defn.Def = {
    val args: Seq[Term.Tuple] = requests.map(_.call)
    q"""
       def bindService[F[_], M[_]](implicit algebra: $algName[F], HTask: _root_.freestyle.FSHandler[M, _root_.monix.eval.Task], handler: _root_.freestyle.FSHandler[F, M], ME: _root_.cats.MonadError[M, Throwable], C: _root_.cats.Comonad[M], S: _root_.monix.execution.Scheduler): _root_.io.grpc.ServerServiceDefinition =
           new freestyle.rpc.internal.service.GRPCServiceDefBuilder(${Lit.String(algName.value)}, ..$args).apply
     """
  }

  val clientName: Type.Name = Type.Name("Client")

  val client: Class = {
    val clientDefs: Seq[Defn.Def] = requests.map(_.clientDef)
    q"""
       class $clientName[M[_]](channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)
          (implicit AC : _root_.freestyle.async.AsyncContext[M], H: FSHandler[_root_.monix.eval.Task, M], E: _root_.scala.concurrent.ExecutionContext)
          extends _root_.io.grpc.stub.AbstractStub[$clientName[M]](channel, options) {

          override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): $clientName[M] = {
              new ${clientName.ctorRef(Ctor.Name(clientName.value))}[M](channel, options)
          }

          ..$clientDefs

       }
     """
  }

  val clientInstance =
    q"""
       def client[M[_]: _root_.freestyle.async.AsyncContext](
          channel: _root_.io.grpc.Channel, 
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)
          (implicit H: FSHandler[_root_.monix.eval.Task, M], E: _root_.scala.concurrent.ExecutionContext) : $clientName[M] =
             new ${clientName.ctorRef(Ctor.Name(clientName.value))}[M](channel, options)
     """

}

private[internal] case class RPCRequest(
    algName: Type.Name,
    name: Term.Name,
    serialization: SerializationType,
    streamingType: Option[StreamingType],
    requestType: Type,
    responseType: Type) {

  val descriptorName: Term.Name = name.copy(value = name.value + "MethodDescriptor")

  val encodersImport: Import = serialization match {
    case Protobuf =>
      q"import _root_.pbdirect._, _root_.freestyle.rpc.internal.service.encoders.pbd._"
    case Avro =>
      q"import _root_.freestyle.rpc.internal.service.encoders.avro._"
  }

  def methodDescriptor =
    q"""
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

  val clientDef: Defn.Def = streamingType match {
    case Some(RequestStreaming) =>
      q"""
         def $name(input: _root_.monix.reactive.Observable[$requestType]): M[$responseType] =
           _root_.freestyle.rpc.internal.client.calls.clientStreaming(input, $descriptorName, channel, options)
       """
    case Some(ResponseStreaming) =>
      q"""
         def $name(request: $requestType): _root_.monix.reactive.Observable[$responseType] =
           _root_.freestyle.rpc.internal.client.calls.serverStreaming(request, $descriptorName, channel, options)
       """
    case Some(BidirectionalStreaming) =>
      q"""
         def $name(input: _root_.monix.reactive.Observable[$requestType]): _root_.monix.reactive.Observable[$responseType] =
           _root_.freestyle.rpc.internal.client.calls.bidiStreaming(input, $descriptorName, channel, options)
       """
    case None =>
      q"""
         def $name(request: $requestType): M[$responseType] =
           _root_.freestyle.rpc.internal.client.calls.unary(request, $descriptorName, channel, options)
      """
  }

  val call: Term.Tuple = streamingType match {
    case Some(RequestStreaming) =>
      q"""
         ($descriptorName,
         _root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(_root_.freestyle.rpc.internal.service.calls.clientStreamingMethod(algebra.$name)))
       """
    case Some(ResponseStreaming) =>
      q"""
         ($descriptorName,
         _root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(_root_.freestyle.rpc.internal.service.calls.serverStreamingMethod(algebra.$name)))
       """
    case Some(BidirectionalStreaming) =>
      q"""
         ($descriptorName,
         _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(_root_.freestyle.rpc.internal.service.calls.bidiStreamingMethod(algebra.$name)))
       """
    case None =>
      q"""
          ($descriptorName,
         _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(_root_.freestyle.rpc.internal.service.calls.unaryMethod(algebra.$name)))
       """
  }

}

private[internal] object utils {

  private[internal] def methodType(s: Option[StreamingType]): Term.Select = s match {
    case Some(RequestStreaming)  => q"_root_.io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING"
    case Some(ResponseStreaming) => q"_root_.io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING"
    case Some(BidirectionalStreaming) =>
      q"_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING"
    case None => q"_root_.io.grpc.MethodDescriptor.MethodType.UNARY"
  }

  private[internal] def serializationType(s: Term.Arg): SerializationType = s match {
    case q"Protobuf" => Protobuf
    case q"Avro"     => Avro
  }

}

private[internal] object errors {
  val invalid = "Invalid use of `@service`"
  val abstractOnly =
    "`@service` can only annotate a trait or abstract class already annotated with @free"
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
