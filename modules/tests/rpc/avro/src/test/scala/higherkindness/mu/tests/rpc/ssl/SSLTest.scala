/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.tests.rpc.ssl

import cats.effect.{IO, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.OverrideAuthority
import higherkindness.mu.rpc.channel.netty._
import higherkindness.mu.rpc.protocol.{Gzip, Identity}
import higherkindness.mu.rpc.server._
import higherkindness.mu.rpc.server.netty.SetSslContext
import io.grpc.{CallOptions, StatusRuntimeException}
import io.grpc.netty.{GrpcSslContexts, NegotiationType}
import io.grpc.internal.testing.TestUtils
import io.netty.handler.ssl.{ClientAuth, SslContext, SslProvider}
import java.io.File
import java.security.cert.X509Certificate
import munit.CatsEffectSuite
import scala.concurrent.duration._
import scala.util.Random
import java.nio.file.Files

class SSLTest extends CatsEffectSuite {

  implicit val service: SSLService[IO] = new ServiceImpl

  // These files are loaded from the classpath (they are inside the grpc-testing jar)
  val serverCertFile: File                         = TestUtils.loadCert("server1.pem")
  val serverPrivateKeyFile: File                   = TestUtils.loadCert("server1.key")
  val serverTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  val serverSslContext: SslContext =
    GrpcSslContexts
      .configure(
        GrpcSslContexts.forServer(serverCertFile, serverPrivateKeyFile),
        SslProvider.OPENSSL
      )
      .trustManager(serverTrustedCaCerts: _*)
      .clientAuth(ClientAuth.REQUIRE)
      .build()

  val grpcConfigs: Resource[IO, List[GrpcConfig]] =
    SSLService
      .bindService[IO]
      .map(service => List(SetSslContext(serverSslContext), AddService(service)))

  def mkServer(port: Int): Resource[IO, GrpcServer[IO]] =
    grpcConfigs.evalMap(GrpcServer.netty[IO](port, _)).flatMap { s =>
      GrpcServer.serverResource(s).as(s)
    }

  val server =
    for {
      p <- Resource.eval(IO(51000 + Random.nextInt(10000)))
      s <- mkServer(p)
    } yield s

  val serverPort = server.evalMap(_.getPort)

  // These files are loaded from the classpath (they are inside the grpc-testing jar)
  val clientCertChainFile: File                    = TestUtils.loadCert("client.pem")
  val clientPrivateKeyFile: File                   = TestUtils.loadCert("client.key")
  val clientTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  val clientSslContext: SslContext =
    GrpcSslContexts.forClient
      .keyManager(clientCertChainFile, clientPrivateKeyFile)
      .trustManager(clientTrustedCaCerts: _*)
      .build()

  def buildChannelInterpreter(
      port: Int,
      configs: List[NettyChannelConfig]
  ): NettyChannelInterpreter =
    new NettyChannelInterpreter(
      ChannelForAddress("localhost", port),
      List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
      configs
    )

  test("server smoke test") {
    server.use(_.isShutdown).assertEquals(false)
  }

  test("TLS happy path") {
    val client: Resource[IO, SSLService[IO]] =
      serverPort.flatMap { p =>
        val channelInterpreter = buildChannelInterpreter(
          p,
          List(
            NettyUsePlaintext(),
            NettyNegotiationType(NegotiationType.TLS),
            NettySslContext(clientSslContext)
          )
        )
        SSLService.clientFromChannel[IO](IO(channelInterpreter.build))
      }

    client
      .use(_.hello(Request(123)))
      .assertEquals(Response(123))
  }

  test(
    "mu-rpc client with TLS connection should " +
      "throw a io.grpc.StatusRuntimeException when no SSLContext is provided"
  ) {
    val client: Resource[IO, SSLService[IO]] =
      serverPort.flatMap { p =>
        val channelInterpreter = buildChannelInterpreter(
          p,
          List(
            NettyUsePlaintext(),
            NettyNegotiationType(NegotiationType.TLS)
          )
        )
        SSLService.clientFromChannel[IO](IO(channelInterpreter.build))
      }

    client
      .use(_.hello(Request(123)))
      .intercept[StatusRuntimeException]
  }

  test(
    "mu-rpc client with SSL/TSL connection should " +
      "throw a io.grpc.StatusRuntimeException when negotiation is skipped"
  ) {
    val client: Resource[IO, SSLService[IO]] =
      serverPort.flatMap { p =>
        val channelInterpreter = buildChannelInterpreter(
          p,
          List(
            NettyUsePlaintext(),
            NettySslContext(clientSslContext)
          )
        )
        SSLService.clientFromChannel[IO](IO(channelInterpreter.build))
      }

    client
      .use(_.hello(Request(123)))
      .intercept[StatusRuntimeException]
  }

}
