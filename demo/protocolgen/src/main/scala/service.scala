package freestyle
package rpc
package demo
package protocolgen

import freestyle.internal.EffectLike
import freestyle.rpc.protocol._
import io.grpc.stub.StreamObserver

@message
case class MessageRequest(name: String, n: Int)

@message
case class MessageReply(name: String, n: Int)

@service
trait GreetingService[F[_]] extends EffectLike[F] {

  @rpc def sayHello(msg: MessageRequest): FS[MessageReply]

  @rpc @stream[ResponseStreaming]
  def lotsOfReplies(msg: MessageRequest, observer: StreamObserver[MessageReply]): FS[Unit]

  @rpc @stream[RequestStreaming]
  def lotsOfGreetings(
      @stream msg: StreamObserver[MessageReply]): FS[StreamObserver[MessageRequest]]

  @rpc @stream[BidirectionalStreaming]
  def bidiHello(@stream msg: StreamObserver[MessageReply]): FS[StreamObserver[MessageRequest]]
}
