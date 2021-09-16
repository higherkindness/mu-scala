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
package ssl

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.channel.OverrideAuthority
import higherkindness.mu.rpc.channel.netty._
import higherkindness.mu.rpc.common._
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.NegotiationType
import munit.CatsEffectSuite

class RPCTests extends CatsEffectSuite {

  import higherkindness.mu.rpc.ssl.Utils._
  import higherkindness.mu.rpc.ssl.Utils.database._
  import higherkindness.mu.rpc.ssl.Utils.service._
  import higherkindness.mu.rpc.ssl.Utils.implicits._

  val serverFixture = ResourceSuiteLocalFixture("rpc-server", grpcServer)

  override def munitFixtures = List(serverFixture)

  test("mu-rpc server should allow to startup a server and check if it's alive") {
    IO(serverFixture()).flatMap(_.isShutdown).assertEquals(false)
  }

  test("mu-rpc server should allow to get the port where it's running") {
    IO(serverFixture()).flatMap(_.getPort).assertEquals(SC.port)
  }

  test(
    "mu-rpc client should work with SSL/TSL connection should " +
      "work when certificates are valid"
  ) {

    val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
      createChannelFor,
      List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
      List(
        NettyUsePlaintext(),
        NettyNegotiationType(NegotiationType.TLS),
        NettySslContext(clientSslContext)
      )
    )

    val avroRpcService: Resource[IO, AvroRPCService[IO]] =
      AvroRPCService.clientFromChannel[IO](IO(channelInterpreter.build))

    val avroWithSchemaRpcService: Resource[
      IO,
      AvroWithSchemaRPCService[IO]
    ] =
      AvroWithSchemaRPCService.clientFromChannel[IO](IO(channelInterpreter.build))

    avroRpcService.use(_.unary(a1)).assertEquals(c1) *>
      avroWithSchemaRpcService.use(_.unaryWithSchema(a1)).assertEquals(c1)
  }

  test(
    "mu-rpc client with SSL/TSL connection should " +
      "throw a io.grpc.StatusRuntimeException when no SSLContext is provided"
  ) {

    val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
      createChannelFor,
      List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
      List(
        NettyUsePlaintext(),
        NettyNegotiationType(NegotiationType.TLS)
      )
    )

    val avroRpcService: Resource[IO, AvroRPCService[IO]] =
      AvroRPCService.clientFromChannel[IO](IO(channelInterpreter.build))
    val avroWithSchemaRpcService: Resource[IO, AvroWithSchemaRPCService[
      IO
    ]] =
      AvroWithSchemaRPCService.clientFromChannel[IO](IO(channelInterpreter.build))

    interceptIO[io.grpc.StatusRuntimeException](avroRpcService.use(_.unary(a1))) *>
      interceptIO[io.grpc.StatusRuntimeException](
        avroWithSchemaRpcService.use(_.unaryWithSchema(a1))
      )
  }

  test(
    "mu-rpc client with SSL/TSL connection should " +
      "throw a io.grpc.StatusRuntimeException when negotiation is skipped"
  ) {

    val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
      createChannelFor,
      List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
      List(
        NettyUsePlaintext(),
        NettySslContext(clientSslContext)
      )
    )

    val avroRpcService: Resource[IO, AvroRPCService[IO]] =
      AvroRPCService.clientFromChannel[IO](IO(channelInterpreter.build))
    val avroWithSchemaRpcService: Resource[IO, AvroWithSchemaRPCService[
      IO
    ]] =
      AvroWithSchemaRPCService.clientFromChannel[IO](IO(channelInterpreter.build))

    interceptIO[io.grpc.StatusRuntimeException](avroRpcService.use(_.unary(a1))) *>
      interceptIO[io.grpc.StatusRuntimeException](
        avroWithSchemaRpcService.use(_.unaryWithSchema(a1))
      )
  }

}
