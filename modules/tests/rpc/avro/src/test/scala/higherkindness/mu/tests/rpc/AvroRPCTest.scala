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

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.protocol.{Gzip, Identity}
import higherkindness.mu.rpc.server._
import io.grpc.CallOptions
import munit.CatsEffectSuite
import scala.util.Random

class AvroRPCTest extends CatsEffectSuite {

  implicit val service: AvroRPCService[IO] = new ServiceImpl

  for (compression <- List(Identity, Gzip)) {

    def mkServer(port: Int): Resource[IO, GrpcServer[IO]] =
      for {
        serviceDefn <- AvroRPCService._bindService[IO](compression)
        server      <- GrpcServer.defaultServer[IO](port, List(AddService(serviceDefn)))
      } yield server

    def mkClient(port: Int): Resource[IO, AvroRPCService[IO]] =
      AvroRPCService.client[IO](
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
        .use(_.hello(TestData.request))
        .assertEquals(Response(TestData.request.a, TestData.request.d))
    }

  }

}
