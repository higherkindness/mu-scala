package higherkindness.mu.tests.rpc.metrics

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.tests.rpc.metrics.{MetricsTestService, Request, Response}
import io.grpc.MethodDescriptor.MethodType
import higherkindness.mu.rpc.testing.servers._

object Services {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  val error: Throwable = new RuntimeException("BOOM!")

  val serviceOp1Info: GrpcMethodInfo =
    GrpcMethodInfo(
      "higherkindness.mu.tests.rpc.MetricsTestService",
      "higherkindness.mu.tests.rpc.MetricsTestService/serviceOp1",
      "serviceOp1",
      MethodType.UNARY
    )

  val serviceOp2Info: GrpcMethodInfo =
    GrpcMethodInfo(
      "higherkindness.mu.tests.rpc.MetricsTestService",
      "higherkindness.mu.tests.rpc.MetricsTestService/serviceOp2",
      "serviceOp2",
      MethodType.UNARY
    )

  val protoRPCServiceImpl: MetricsTestService[IO] = new MetricsTestService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO(Response())
    def serviceOp2(r: Request): IO[Response] = IO(Response())
  }

  val protoRPCServiceErrorImpl: MetricsTestService[IO] = new MetricsTestService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO.raiseError(error)
    def serviceOp2(r: Request): IO[Response] = IO.raiseError(error)
  }

  def createClient(sc: ServerChannel): Resource[IO, MetricsTestService[IO]] =
    MetricsTestService.clientFromChannel[IO](IO.pure(sc.channel))

}
