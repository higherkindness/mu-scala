package higherkindness.mu.tests.rpc

import _root_.fs2._
import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.protocol.{Gzip, Identity}
import higherkindness.mu.rpc.server._
import higherkindness.mu.tests.models._
import io.grpc.CallOptions
import munit.CatsEffectSuite

class ProtobufRPCTest extends CatsEffectSuite {

  implicit val service: ProtoRPCService[IO] = new ServiceImpl

  val port = 54321

  for (compression <- List(Identity, Gzip)) {

    val server: Resource[IO, GrpcServer[IO]] =
      for {
        serviceDefn <- ProtoRPCService._bindService[IO](compression)
        server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
      } yield server

    val client: Resource[IO, ProtoRPCService[IO]] =
      ProtoRPCService.client[IO](
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
        .use(_.unary(A(1, 2)))
        .assertEquals(C("hello", Some(A(1, 2))))
    }

    test(s"client streaming ($compression)") {
      (server *> client)
        .use(_.clientStreaming(Stream(A(1, 2), A(3, 4), A(5, 6))))
        .assertEquals(D(3))
    }

    test(s"server streaming ($compression)") {
      (server *> client)
        .use { c =>
          Stream.force(c.serverStreaming(B(Some(A(1, 2)), Some(A(3, 4))))).compile.toList
        }
        .assertEquals(List(C("first", Some(A(1, 2))), C("second", Some(A(3, 4)))))
    }

    test(s"client handling of errors in server streaming ($compression)") {
      val a = A(1, 2)
      def clientProgram(errorCode: String, s: ProtoRPCService[IO]): IO[List[C]] =
        s.serverStreamingWithError(E(Some(a), errorCode))
          .map(_.handleErrorWith(ex => Stream(C(ex.getMessage, Some(a)))))
          .flatMap(_.compile.toList)

      (server *> client)
        .use { s =>
          clientProgram("SE", s)
            .assertEquals(List(C("INVALID_ARGUMENT: SE", Some(a)))) *>
            clientProgram("SRE", s)
              .assertEquals(List(C("INVALID_ARGUMENT: SRE", Some(a)))) *>
            clientProgram("RTE", s)
              .assertEquals(List(C("INTERNAL: RTE", Some(a)))) *>
            clientProgram("Thrown", s)
              .assertEquals(List(C("UNKNOWN", Some(a))))
        }
    }

    test(s"bidirectional streaming ($compression)") {
      (server *> client)
        .use { c =>
          val req = Stream(
            E(Some(A(1, 2)), "hello"),
            E(Some(A(3, 4)), "world")
          ).covary[IO]
          Stream.force(c.bidiStreaming(req)).compile.toList
        }
        .assertEquals(List(B(Some(A(1, 2)), Some(A(1, 2))), B(Some(A(3, 4)), Some(A(3, 4)))))
    }

  }

}
