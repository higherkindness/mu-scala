/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.tests.rpc

import fs2.Stream
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.protocol.{Gzip, Identity}
import higherkindness.mu.rpc.server._
import higherkindness.mu.tests.models._
import io.grpc.CallOptions
import munit.CatsEffectSuite
import scala.util.Random

class ProtobufRPCTest extends CatsEffectSuite {

  implicit val service: ProtoRPCService[IO] = new ServiceImpl

  for (compression <- List(Identity, Gzip)) {

    def mkServer(port: Int): Resource[IO, GrpcServer[IO]] =
      for {
        serviceDefn <- ProtoRPCService._bindService[IO](compression)
        server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
      } yield server

    def mkClient(port: Int): Resource[IO, ProtoRPCService[IO]] =
      ProtoRPCService.client[IO](
        ChannelForAddress("localhost", port),
        options = compression match {
          case Identity => CallOptions.DEFAULT
          case Gzip     => CallOptions.DEFAULT.withCompression("gzip")
        }
      )

    val server =
      for {
        p <- Resource.eval(IO(51000 + Random.nextInt(10000)))
        s <- mkServer(p)
      } yield s

    val serverPort = server.evalMap(_.getPort)

    val client = serverPort.flatMap(mkClient)

    test(s"server smoke test ($compression)") {
      server.use(_.isShutdown).assertEquals(false)
    }

    test(s"unary method ($compression)") {
      client
        .use(_.unary(A(1, 2)))
        .assertEquals(C("hello", Some(A(1, 2))))
    }

    test(s"client streaming ($compression)") {
      client
        .use(_.clientStreaming(Stream(A(1, 2), A(3, 4), A(5, 6))))
        .assertEquals(D(3))
    }

    test(s"server streaming ($compression)") {
      client
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

      client
        .use { s =>
          clientProgram("SE", s)
            .assertEquals(List(C("INVALID_ARGUMENT: SE", Some(a)))) *>
            clientProgram("SRE", s)
              .assertEquals(List(C("INVALID_ARGUMENT: SRE", Some(a)))) *>
            clientProgram("RTE", s)
              .assertEquals(List(C("INTERNAL: RTE", Some(a)))) *>
            clientProgram("Thrown", s)
              .assertEquals(List(C("UNKNOWN: Application error processing RPC", Some(a))))
        }
    }

    test(s"bidirectional streaming ($compression)") {
      client
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
