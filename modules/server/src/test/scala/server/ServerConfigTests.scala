/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc
package server

import java.net.InetSocketAddress

import cats.Id
import cats.data.Kleisli
import freestyle.rpc.common.SC
import freestyle.rpc.server.netty.NettyServerConfigBuilder
import io.grpc.Server

import scala.collection.JavaConverters._

class ServerConfigTests extends RpcServerTestSuite {

  import implicits._

  "GrpcConfigInterpreter" should {

    "work as expected" in {

      val kInterpreter =
        new GrpcKInterpreter[Id](serverMock).apply(Kleisli[Id, Server, Int](s => s.getPort))

      kInterpreter shouldBe SC.port
    }

  }

  "SServerBuilder" should {

    "work as expected for a basic configuration" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server               = SServerBuilder(SC.port, configList).build

      server.getServices.asScala.toList shouldBe List(sd1)
    }
  }

  "NettyServerConfigBuilder" should {

    "work as expected for port" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server               = NettyServerConfigBuilder(ChannelForPort(SC.port), configList).build

      server.getServices.asScala.toList shouldBe List(sd1)
    }

    "work as expected for SocketAddress" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server =
        NettyServerConfigBuilder(
          ChannelForSocketAddress(new InetSocketAddress(SC.host, SC.port)),
          configList).build

      server.getServices.asScala.toList shouldBe List(sd1)
    }

    "work as expected for port, with any configuration combination" in {

      val server: Server =
        NettyServerConfigBuilder(ChannelForPort(SC.port), grpcAllConfigList).build

      server.getServices.asScala.toList shouldBe List(sd1)
    }

    "throw an exception when configuration is not recognized" in {

      case object Unexpected extends GrpcConfig

      val configList: List[GrpcConfig] = List(AddService(sd1), Unexpected)

      an[MatchError] shouldBe thrownBy(
        NettyServerConfigBuilder(ChannelForPort(SC.port), configList).build)
    }

    "throw an exception when ChannelFor is not recognized" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))

      an[IllegalArgumentException] shouldBe thrownBy(
        NettyServerConfigBuilder(ChannelForTarget(SC.host), configList).build)
    }
  }

  "ServerW" should {

    "work as expected for the '.netty(ChannelFor)' builder" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server               = ServerW.netty(ChannelForPort(SC.port), configList).server

      server.getServices.asScala.toList shouldBe List(sd1)
    }

    "work as expected for the '.netty(Int)' builder" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server               = ServerW.netty(SC.port, configList).server

      server.getServices.asScala.toList shouldBe List(sd1)
    }
  }

}
