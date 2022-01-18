package higherkindness.mu.rpc.internal.context

import cats.effect._
import io.grpc.{Metadata, MethodDescriptor}

trait ServerContext[F[_], C] {

  def apply[Req, Res](descriptor: MethodDescriptor[Req, Res], metadata: Metadata): Resource[F, C]

}

object ServerContext {
  def impl[F[_], C](f: Metadata => F[C]): ServerContext[F, C] = new ServerContext[F, C] {
    override def apply[Req, Res](
        descriptor: MethodDescriptor[Req, Res],
        metadata: Metadata
    ): Resource[F, C] =
      Resource.eval(f(metadata))
  }
}
