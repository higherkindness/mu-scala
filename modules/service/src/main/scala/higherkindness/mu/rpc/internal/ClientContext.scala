package higherkindness.mu.rpc.internal

import cats.effect.Async
import cats.syntax.all._
import higherkindness.mu.rpc.internal.client.tracingKernelToHeaders
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import natchez.Span

trait ClientContext[F[_], C] {

  def apply[Req, Res](
    descriptor: MethodDescriptor[Req, Res],
    channel: Channel,
    options: CallOptions,
    current: C): F[(C, Metadata)]

}

object TracingClientContext {

  def impl[F[_]: Async]: ClientContext[F, Span[F]] = new ClientContext[F, Span[F]] {
    override def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      current: Span[F]): F[(Span[F], Metadata)] =
    current.span(descriptor.getFullMethodName).use(span => span.kernel.map(tracingKernelToHeaders).fproductLeft(_ => span))

  }

}