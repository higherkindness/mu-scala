package freestyle
package rpc
package demo
package protocolgen

import freestyle.internal.EffectLike
import freestyle.rpc.protocol._
import io.grpc.stub.StreamObserver

@option(name = "java_package", value = "com.example.foo", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Ponycopter", quote = true)
object protocols {

  @message
  case class MessageRequest(name: String, n: Option[Int])

  @message
  case class MessageReply(name: String, n: List[Int])

  @service
  trait GreetingService[F[_]] extends EffectLike[F] {

    @rpc def sayHello(msg: MessageRequest): FS[MessageReply]

    @rpc
    @stream[ResponseStreaming]
    def lotsOfReplies(msg: MessageRequest, observer: StreamObserver[MessageReply]): FS[Unit]

    @rpc
    @stream[RequestStreaming]
    def lotsOfGreetings(
        @stream msg: StreamObserver[MessageReply]): FS[StreamObserver[MessageRequest]]

    @rpc
    @stream[BidirectionalStreaming]
    def bidiHello(@stream msg: StreamObserver[MessageReply]): FS[StreamObserver[MessageRequest]]
  }

}
