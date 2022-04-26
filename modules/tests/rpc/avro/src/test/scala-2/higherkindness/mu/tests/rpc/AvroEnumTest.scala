package higherkindness.mu.tests.rpc

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.server._
import munit.CatsEffectSuite

class AvroEnumTest extends CatsEffectSuite {

  implicit val service: AvroRPCService[IO] = new ServiceImpl

  val port = 54323

  val grpcServer: Resource[IO, GrpcServer[IO]] =
    for {
      serviceDefn <- AvroRPCService.bindService[IO]
      server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
    } yield server

  val client: Resource[IO, AvroRPCService[IO]] =
    AvroRPCService.client[IO](ChannelForAddress("localhost", port))

  test("request message containing an enum field") {
    (grpcServer *> client)
      .use(_.helloEnum(TestData.requestWithEnumField)
      .assertEquals(Response(TestData.request.a)))
  }

}
