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

package mu.rpc
package ssl

import mu.rpc.client.OverrideAuthority
import mu.rpc.client.netty.{
  NettyChannelInterpreter,
  NettyNegotiationType,
  NettySslContext,
  NettyUsePlaintext
}
import mu.rpc.common._
import mu.rpc.server._
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.NegotiationType
import org.scalatest._

class RPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import TestsImplicits._
  import mu.rpc.ssl.Utils._
  import mu.rpc.ssl.Utils.database._
  import mu.rpc.ssl.Utils.service._
  import mu.rpc.ssl.Utils.implicits._

  override protected def beforeAll(): Unit =
    serverStart[ConcurrentMonad].unsafeRunSync()

  override protected def afterAll(): Unit =
    serverStop[ConcurrentMonad].unsafeRunSync()

  "mu-rpc server" should {

    "allow to startup a server and check if it's alive" in {

      def check[F[_]](implicit S: GrpcServer[F]): F[Boolean] =
        S.isShutdown

      check[ConcurrentMonad].unsafeRunSync() shouldBe false

    }

    "allow to get the port where it's running" in {

      def check[F[_]](implicit S: GrpcServer[F]): F[Int] =
        S.getPort

      check[ConcurrentMonad].unsafeRunSync() shouldBe SC.port

    }

  }

  "mu-rpc client should work with SSL/TSL connection" should {

    "work when certificates are valid" in {

      val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
        createChannelFor,
        List(
          OverrideAuthority(TestUtils.TEST_SERVER_HOST),
          NettyUsePlaintext(),
          NettyNegotiationType(NegotiationType.TLS),
          NettySslContext(clientSslContext)
        )
      )

      val avroRpcService: AvroRPCService.Client[ConcurrentMonad] =
        AvroRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)
      val avroWithSchemaRpcService: AvroWithSchemaRPCService.Client[ConcurrentMonad] =
        AvroWithSchemaRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)

      avroRpcService.unary(a1).unsafeRunSync() shouldBe c1
      avroWithSchemaRpcService.unaryWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "io.grpc.StatusRuntimeException is thrown when no SSLContext is provided" in {

      val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
        createChannelFor,
        List(
          OverrideAuthority(TestUtils.TEST_SERVER_HOST),
          NettyUsePlaintext(),
          NettyNegotiationType(NegotiationType.TLS)
        )
      )

      val avroRpcService: AvroRPCService.Client[ConcurrentMonad] =
        AvroRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)
      val avroWithSchemaRpcService: AvroWithSchemaRPCService.Client[ConcurrentMonad] =
        AvroWithSchemaRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)

      a[io.grpc.StatusRuntimeException] shouldBe thrownBy(avroRpcService.unary(a1).unsafeRunSync())

      a[io.grpc.StatusRuntimeException] shouldBe thrownBy(
        avroWithSchemaRpcService.unaryWithSchema(a1).unsafeRunSync())

    }

    "io.grpc.StatusRuntimeException is thrown when negotiation is skipped" in {

      val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
        createChannelFor,
        List(
          OverrideAuthority(TestUtils.TEST_SERVER_HOST),
          NettyUsePlaintext(),
          NettySslContext(clientSslContext)
        )
      )

      val avroRpcService: AvroRPCService.Client[ConcurrentMonad] =
        AvroRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)
      val avroWithSchemaRpcService: AvroWithSchemaRPCService.Client[ConcurrentMonad] =
        AvroWithSchemaRPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)

      a[io.grpc.StatusRuntimeException] shouldBe thrownBy(avroRpcService.unary(a1).unsafeRunSync())

      a[io.grpc.StatusRuntimeException] shouldBe thrownBy(
        avroWithSchemaRpcService.unaryWithSchema(a1).unsafeRunSync())

    }

  }

}
