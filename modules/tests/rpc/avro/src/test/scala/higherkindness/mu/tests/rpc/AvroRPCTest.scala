package higherkindness.mu.tests.rpc

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.server._
import munit.CatsEffectSuite

class AvroRPCTest extends CatsEffectSuite {

  implicit val service: AvroRPCService[IO] = new ServiceImpl

  val port = 54322

  val grpcServer: Resource[IO, GrpcServer[IO]] =
    for {
      serviceDefn <- AvroRPCService.bindService[IO]
      server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
    } yield server

  val client: Resource[IO, AvroRPCService[IO]] =
    AvroRPCService.client[IO](ChannelForAddress("localhost", port))

  test("server smoke test") {
    grpcServer.use(_.isShutdown).assertEquals(false)
  }

  test("unary method") {
    (grpcServer *> client)
      .use(_.hello(TestData.request)
      .assertEquals(Response(TestData.request.a)))
  }

}
