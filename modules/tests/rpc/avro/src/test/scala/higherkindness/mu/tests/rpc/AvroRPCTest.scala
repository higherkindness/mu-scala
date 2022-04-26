package higherkindness.mu.tests.rpc

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.protocol.{Gzip, Identity}
import higherkindness.mu.rpc.server._
import io.grpc.CallOptions
import munit.CatsEffectSuite

class AvroRPCTest extends CatsEffectSuite {

  implicit val service: AvroRPCService[IO] = new ServiceImpl

  val port = 54322

  for (compression <- List(Identity, Gzip)) {

    val server: Resource[IO, GrpcServer[IO]] =
      for {
        serviceDefn <- AvroRPCService._bindService[IO](compression)
        server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
      } yield server

    val client: Resource[IO, AvroRPCService[IO]] =
      AvroRPCService.client[IO](
        ChannelForAddress("localhost", port),
        options = compression match {
          case Identity => CallOptions.DEFAULT
          case Gzip     => CallOptions.DEFAULT.withCompression("gzip")
        }
      )

    test(s"server smoke test ($compression)") {
      server.use(_.isShutdown).assertEquals(false)
    }

    test(s"unary method ($compression)") {
      (server *> client)
        .use(_.hello(TestData.request))
        .assertEquals(Response(TestData.request.a))
    }

  }

}
