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
class OperationModels[C <: Context](val c: C) {
  import c.universe._

  val wartSuppression = new WartSuppression[c.type](c)
  import wartSuppression._

  val typeAnalysis = new TypeAnalysis[c.type](c)
  import typeAnalysis._

  val treeHelpers = new TreeHelpers[c.type](c)
  import treeHelpers._

  case class Operation(name: TermName, request: TypeTypology, response: TypeTypology) {

    val isStreaming: Boolean = request.isStreaming || response.isStreaming

    val streamingType: Option[StreamingType] = (request.isStreaming, response.isStreaming) match {
      case (true, true)  => Some(BidirectionalStreaming)
      case (true, false) => Some(RequestStreaming)
      case (false, true) => Some(ResponseStreaming)
      case _             => None
    }

    val prevalentStreamingTarget: TypeTypology =
      if (streamingType.contains(ResponseStreaming)) response else request

  }

  case class EnclosingService(
      serviceName: TypeName,
      fullServiceName: String,
      compressionType: CompressionType,
      methodNameStyle: MethodNameStyle,
      F: TypeName
  )

  // todo: validate that the request and responses are case classes, if possible
  case class RPCMethod(
      service: EnclosingService,
      operation: Operation
  ) {

    import service._
    import operation._

    private def kleisliFContextB(C: TypeName, B: Tree) =
      tq"_root_.cats.data.Kleisli[$F, $C, $B]"

    // Type lambda for Kleisli[F, C, *].
    private def kleisliFContext(C: TypeName): SelectFromTypeTree =
      tq"({ type T[α] = _root_.cats.data.Kleisli[$F, $C, α] })#T"

    private val compressionTypeTree: Tree =
      q"_root_.higherkindness.mu.rpc.protocol.${TermName(compressionType.toString)}"

    private val dispatcherValueName: Tree = q"disp"

    private val clientCallsImpl = prevalentStreamingTarget match {
      case _: Fs2StreamTpe => q"_root_.higherkindness.mu.rpc.internal.client.fs2.calls"
      case _               => q"_root_.higherkindness.mu.rpc.internal.client.calls"
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

    private val reqType         = request.originalType
    private val reqElemType     = request.messageType
    private val wrappedRespType = response.originalType
    private val respElemType    = response.messageType

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
      q"""
      $clientCallsImpl.${TermName(clientMethodName)}[$F, $reqElemType, $respElemType](
        input,
        $methodDescriptorName.$methodDescriptorValName,
        channel,
        options
      )
      """

    private def clientContextCallMethodFor(C: TypeName, clientMethodName: String) =
      q"""
      $clientCallsImpl.${TermName(clientMethodName)}[$F, $C, $reqElemType, $respElemType](
        input,
        $methodDescriptorName.$methodDescriptorValName,
        channel,
        options
      )
      """

    val clientDef: Tree = {
      def method(clientCallMethodName: String) =
        q"""
        def $name(input: $reqType): $wrappedRespType =
          ${clientCallMethodFor(clientCallMethodName)}
        """

      streamingType match {
        case Some(RequestStreaming)       => method("clientStreaming")
        case Some(ResponseStreaming)      => method("serverStreaming")
        case Some(BidirectionalStreaming) => method("bidiStreaming")
        case None                         => method("unary")
      }
    }

    // Kleisli[F, C, Resp]
    private def kleisliContextFResp(C: TypeName) = kleisliFContextB(C, respElemType)

    def contextClientDef(C: TypeName): Tree = (streamingType, prevalentStreamingTarget) match {
      case (None, _) =>
        // def foo(input: Req): Kleisli[F, MC, Resp]
        q"""
        def $name(input: $reqType): ${kleisliContextFResp(C)} =
          ${clientContextCallMethodFor(C, "contextUnary")}
        """
      case (Some(RequestStreaming), _: Fs2StreamTpe) =>
        // def foo(input: Stream[Kleisli[F, MC, *], Req]): Kleisli[F, MC, Resp]
        q"""
        def $name(input: _root_.fs2.Stream[${kleisliFContext(
            C
          )}, $reqElemType]): ${kleisliContextFResp(C)} =
          ${clientContextCallMethodFor(C, "contextClientStreaming")}
        """
      case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
        // def foo(input: Req): Kleisli[F, MC, Stream[Kleisli[F, MC, *], Resp]]
        val returnType =
          kleisliFContextB(C, tq"_root_.fs2.Stream[${kleisliFContext(C)}, $respElemType]")
        q"""
        def $name(input: $reqType): $returnType =
          ${clientContextCallMethodFor(C, "contextServerStreaming")}
        """
      case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
        // def foo(input: Stream[Kleisli[F, MC, *], Req]): Stream[Kleisli[F, MC, *], Resp]
        val returnType =
          kleisliFContextB(C, tq"_root_.fs2.Stream[${kleisliFContext(C)}, $respElemType]")
        q"""
        def $name(input: _root_.fs2.Stream[${kleisliFContext(C)}, $reqElemType]): $returnType =
          ${clientContextCallMethodFor(C, "contextBidiStreaming")}
        """
      case _ =>
        c.abort(
          c.enclosingPosition,
          s"Unable to define a context client method for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service $serviceName"
        )
    }

    val serverCallHandler: Tree = (streamingType, prevalentStreamingTarget) match {
      case (Some(RequestStreaming), _: Fs2StreamTpe) =>
        q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[$F, $reqElemType, $respElemType](
          { (req: _root_.fs2.Stream[$F, $reqElemType], $anonymousParam) => algebra.$name(req) },
          $dispatcherValueName,
          $compressionTypeTree
        )
        """

      case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
        q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[$F, $reqElemType, $respElemType](
          { (req: $reqType, $anonymousParam) => algebra.$name(req) },
          $dispatcherValueName,
          $compressionTypeTree
        )
        """

      case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
        q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[$F, $reqElemType, $respElemType](
          { (req: _root_.fs2.Stream[$F, $reqElemType], $anonymousParam) => algebra.$name(req) },
          $dispatcherValueName,
          $compressionTypeTree
        )
        """

      case (None, _) =>
        q"""
        _root_.higherkindness.mu.rpc.internal.server.handlers.unary[$F, $reqElemType, $respElemType](
          algebra.$name,
          $compressionTypeTree,
          $dispatcherValueName
        )
        """
      case _ =>
        c.abort(
          c.enclosingPosition,
          s"Unable to define a handler for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service ${service.serviceName}"
        )
    }

    val descriptorAndHandler: Tree =
      q"($methodDescriptorName.$methodDescriptorValName, $serverCallHandler)"

    def contextServerCallHandler(C: TypeName): Tree =
      (streamingType, prevalentStreamingTarget) match {
        case (Some(RequestStreaming), _: Fs2StreamTpe) =>
          q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[$F, $C, $reqElemType, $respElemType](
          algebra.$name _,
          $methodDescriptorName.$methodDescriptorValName,
          $dispatcherValueName,
          $compressionTypeTree
        )
        """
        case (Some(ResponseStreaming), _: Fs2StreamTpe) =>
          q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[$F, $C, $reqElemType, $respElemType](
          algebra.$name _,
          $methodDescriptorName.$methodDescriptorValName,
          $dispatcherValueName,
          $compressionTypeTree
        )
        """
        case (Some(BidirectionalStreaming), _: Fs2StreamTpe) =>
          q"""
        _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[$F, $C, $reqElemType, $respElemType](
          algebra.$name _,
          $methodDescriptorName.$methodDescriptorValName,
          $dispatcherValueName,
          $compressionTypeTree
        )
        """
        case (None, _) =>
          q"""
        _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[$F, $C, $reqElemType, $respElemType](
          algebra.$name,
          $methodDescriptorName.$methodDescriptorValName,
          $compressionTypeTree,
          $dispatcherValueName
        )
        """
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"Unable to define a context handler for the streaming type $streamingType and $prevalentStreamingTarget for the method $name in the service ${service.serviceName}"
          )
      }

    def descriptorAndContextHandler(C: TypeName): Tree =
      q"($methodDescriptorName.$methodDescriptorValName, ${contextServerCallHandler(C)})"

  }

}
// $COVERAGE-ON$
