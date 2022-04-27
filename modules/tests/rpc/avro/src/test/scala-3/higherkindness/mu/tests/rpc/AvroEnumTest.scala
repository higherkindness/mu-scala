package higherkindness.mu.tests.rpc

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.server._
import io.grpc.StatusRuntimeException
import munit.CatsEffectSuite
import scala.util.Random

class AvroEnumTest extends CatsEffectSuite {

  implicit val service: AvroRPCService[IO] = new ServiceImpl

  val port = 51000 + Random.nextInt(10000)

  val grpcServer: Resource[IO, GrpcServer[IO]] =
    for {
      serviceDefn <- AvroRPCService.bindService[IO]
      server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
    } yield server

  val client: Resource[IO, AvroRPCService[IO]] =
    AvroRPCService.client[IO](ChannelForAddress("localhost", port))

  test("avro4s cannot decode a request message containing an enum field") {
    // see https://github.com/sksamuel/avro4s/pull/717 for the fix for this issue
    (grpcServer *> client)
      .use(_.helloEnum(TestData.requestWithEnumField))
      .intercept[StatusRuntimeException]
  }

}
