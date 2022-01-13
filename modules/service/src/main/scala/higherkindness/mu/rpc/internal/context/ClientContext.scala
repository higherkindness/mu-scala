package higherkindness.mu.rpc.internal.context

import cats.effect.Resource
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}

final case class ClientContextMetaData[C](context: C, metadata: Metadata)

trait ClientContext[F[_], C] {

  def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      current: C
  ): Resource[F, ClientContextMetaData[C]]

}
