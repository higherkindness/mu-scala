/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc
package server

import java.net.InetSocketAddress
import cats.effect.IO
import higherkindness.mu.rpc.common.SC
import munit.CatsEffectSuite

class ServerConfigTests extends CatsEffectSuite {

  import TestData._

  test(
    "GrpcServer.default should " +
      "work as expected for a basic configuration"
  ) {

    val configList: List[GrpcConfig] = List(AddService(sd1))
    val server: IO[GrpcServer[IO]]   = GrpcServer.default(SC.port, configList)

    server.flatMap(_.getServices).assertEquals(List(sd1))
  }

  test("GrpcServer.netty should work as expected for port") {

    val configList: List[GrpcConfig] = List(AddService(sd1))
    val server: IO[GrpcServer[IO]]   = GrpcServer.netty(ChannelForPort(SC.port), configList)

    server.flatMap(_.getServices).assertEquals(List(sd1))
  }

  test("GrpcServer.netty should work as expected for SocketAddress") {

    val configList: List[GrpcConfig] = List(AddService(sd1))
    val server: IO[GrpcServer[IO]] =
      GrpcServer.netty(
        ChannelForSocketAddress(new InetSocketAddress(SC.host, SC.port)),
        configList
      )

    server.flatMap(_.getServices).assertEquals(List(sd1))
  }

  test(
    "GrpcServer.netty should " +
      "work as expected for port, with any configuration combination"
  ) {

    val server: IO[GrpcServer[IO]] =
      GrpcServer.netty(ChannelForPort(SC.port), grpcAllConfigList)

    server.flatMap(_.getServices).assertEquals(List(sd1))
  }

  test(
    "GrpcServer.netty should " +
      "work as expected for an `Int` port"
  ) {

    val configList: List[GrpcConfig] = List(AddService(sd1))
    val server: IO[GrpcServer[IO]]   = GrpcServer.netty[IO](SC.port, configList)

    server.flatMap(_.getServices).assertEquals(List(sd1))
  }

  test(
    "GrpcServer.netty should " +
      "throw an exception when configuration is not recognized"
  ) {

    case object Unexpected extends GrpcConfig

    val configList: List[GrpcConfig] = List(AddService(sd1), Unexpected)

    val server: IO[GrpcServer[IO]] =
      GrpcServer.netty(ChannelForPort(SC.port), configList)

    interceptIO[MatchError](server)
  }

  test("GrpcServer.netty should throw an exception when ChannelFor is not recognized") {

    val configList: List[GrpcConfig] = List(AddService(sd1))

    val server: IO[GrpcServer[IO]] =
      GrpcServer.netty(ChannelForTarget(SC.host), configList)

    interceptIO[IllegalArgumentException](server)
  }

}
