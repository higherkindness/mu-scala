package higherkindness.mu.rpc.internal.service.makro

import higherkindness.mu.rpc.protocol._
import scala.reflect.macros.blackbox.Context

class OperationModels[C <: Context, TA <: TypeAnalysis[C]](val c: C, val typeAnalysis: TA) {
  import c.universe._
  import typeAnalysis._

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

}
