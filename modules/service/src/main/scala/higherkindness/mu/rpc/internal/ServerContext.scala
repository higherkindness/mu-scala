package higherkindness.mu.rpc.internal

import cats.effect._
import higherkindness.mu.rpc.internal.server.extractTracingKernel
import io.grpc.{Metadata, MethodDescriptor}
import natchez.{EntryPoint, Span}

trait ServerContext[F[_], C] {

  def apply[Req, Res](descriptor: MethodDescriptor[Req, Res], metadata: Metadata): Resource[F, C]

}

object TracingServerContext {

  def impl[F[_]](entrypoint: EntryPoint[F]): ServerContext[F, Span[F]] = new ServerContext[F, Span[F]] {
    override def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      metadata: Metadata): Resource[F, Span[F]] =
      entrypoint.continueOrElseRoot(descriptor.getFullMethodName, extractTracingKernel(metadata))

  }

}