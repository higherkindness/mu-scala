package higherkindness.mu.rpc.internal

import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}

trait ClientContext[F[_], C] {

  def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      current: C
  ): F[(C, Metadata)]

}
