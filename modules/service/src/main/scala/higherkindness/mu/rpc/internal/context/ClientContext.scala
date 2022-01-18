package higherkindness.mu.rpc.internal.context

import cats.Monad
import cats.effect.Resource
import cats.syntax.all._
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

object ClientContext {
  def impl[F[_]: Monad, C](f: (C, Metadata) => F[Unit]): ClientContext[F, C] =
    new ClientContext[F, C] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          channel: Channel,
          options: CallOptions,
          current: C
      ): Resource[F, ClientContextMetaData[C]] = Resource.eval {
        new Metadata().pure[F].flatTap(f(current, _)).map(ClientContextMetaData(current, _))
      }
    }
}
