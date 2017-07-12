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

package freestyle.rpc.internal.service

import freestyle.internal.ScalametaUtil
import freestyle.rpc.protocol.{
  BidirectionalStreaming,
  RequestStreaming,
  ResponseStreaming,
  StreamingType
}

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object serviceImpl {

  import errors._

//  implicit val mirror = Mirror()
//
//  println(mirror.database)

  def service(defn: Any): Stat = defn match {
    case Term.Block(Seq(cls: Trait, companion: Object)) =>
      serviceExtras(cls, companion)
    case Term.Block(Seq(cls: Class, companion: Object)) if ScalametaUtil.isAbstract(cls) =>
      serviceExtras(cls, companion)
    case _ =>
      println(defn.getClass)
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
          val result = q"..$mods object $ename extends $enrichedTemplate"
          println(result)
          result
      }
  }

  def enrich(serviceAlg: ServiceAlg, members: Seq[Stat]): Seq[Stat] =
    members ++ serviceAlg.methodDescriptors :+ serviceAlg.serviceBindings

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

  val requests: List[RPCRequest] = template.stats.toList.flatten.collect {
    case q"@rpc @stream[ResponseStreaming.type] def $name[..$tparams]($request, observer: StreamObserver[$response]): FS[Unit]" =>
      RPCRequest(algName, name, Some(ResponseStreaming), paramTpe(request), response)
    case q"@rpc @stream[RequestStreaming.type] def $name[..$tparams]($request): FS[StreamObserver[$response]]" =>
      RPCRequest(algName, name, Some(RequestStreaming), paramTpe(request), response)
    case q"@rpc @stream[BidirectionalStreaming.type] def $name[..$tparams](param: StreamObserver[$request]): FS[StreamObserver[$response]]" =>
      RPCRequest(algName, name, Some(BidirectionalStreaming), request, response)
    case q"@rpc def $name[..$tparams]($request): FS[$response]" =>
      RPCRequest(algName, name, None, paramTpe(request), response)
  }

  val methodDescriptors: Seq[Defn.Val] = requests.map(_.methodDescriptor)

  val serviceBindings: Defn.Def = {
    val args: Seq[Term.Tuple] = requests.map(_.call)
    q"""
       def bindService[M[_]](implicit handler: Handler[M]): _root_.io.grpc.ServerServiceDefinition =
           new freestyle.rpc.internal.service.GRPCServiceDefBuilder.apply(${Lit.String(
      algName.value)}, ..$args)

     """
  }

  def recurseApply(select: Term.Select, requests: List[RPCRequest]): Term.Select = {
    requests match {
      case Nil    => select
      case h :: t => recurseApply(select.copy(h.call), t)
    }
  }

}

private[internal] case class RPCRequest(
    algName: Type.Name,
    name: Term.Name,
    streamingType: Option[StreamingType],
    requestType: Type,
    responseType: Type) {

  def methodDescriptor =
    q"""
        val ${Pat.Var.Term(name)}: _root_.io.grpc.MethodDescriptor[$requestType, $responseType] =
          _root_.io.grpc.MethodDescriptor.create(
          ${utils.methodType(streamingType)},
          _root_.io.grpc.MethodDescriptor.generateFullMethodName(${Lit.String(algName.value)}, ${Lit
      .String(name.value)}),
            implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$requestType]],
            implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$responseType]])
      """

  val call: Term.Tuple = streamingType match {
    case Some(RequestStreaming) =>
      q"""
         ($name,
         _root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(new _root_.io.grpc.stub.ServerCalls.ClientStreamingMethod[$responseType, $requestType] {
                 override def invoke(observer: _root_.io.grpc.stub.StreamObserver[$requestType]): _root_.io.grpc.stub.StreamObserver[$responseType] =
                   handler.$name(observer)
         }))
       """
    case Some(ResponseStreaming) =>
      q"""
         ($name,
         _root_.io.grpc.stub.ServerCalls.asyncServerStreamingCall(new _root_.io.grpc.stub.ServerCalls.ServerStreamingMethod[$requestType, $responseType] {
                 override def invoke(request: $requestType, observer: _root_.io.grpc.stub.StreamObserver[$responseType]): Unit =
                   handler.$name(request, observer)
         }))
       """
    case Some(BidirectionalStreaming) =>
      q"""
         ($name,
         _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[$responseType, $requestType] {
                 override def invoke(observer: _root_.io.grpc.stub.StreamObserver[$requestType]): _root_.io.grpc.stub.StreamObserver[$responseType] =
                   handler.$name(observer)
         }))
       """
    case None =>
      q"""
          ($name,
         _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[$requestType, $responseType] {
                 override def invoke(request: $requestType, observer: _root_.io.grpc.stub.StreamObserver[$responseType]): Unit =
                   handler.$name(request).onComplete(com.trueaccord.scalapb.grpc.Grpc.completeObserver(observer))(executionContext)
         }))
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

}

private[internal] object errors {
  val invalid = "Invalid use of `@service`"
  val abstractOnly =
    "`@service` can only annotate a trait or abstract class already annotated with @free"
}
// $COVERAGE-ON$
