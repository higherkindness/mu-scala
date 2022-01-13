package higherkindness.mu.rpc.internal.context

import cats.effect._
import io.grpc.{Metadata, MethodDescriptor}

trait ServerContext[F[_], C] {

  def apply[Req, Res](descriptor: MethodDescriptor[Req, Res], metadata: Metadata): Resource[F, C]

}
