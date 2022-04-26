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
import scala.util.Random

class SSLTest extends CatsEffectSuite {

  val port = 51000 + Random.nextInt(2000)

  implicit val service: SSLService[IO] = new ServiceImpl

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

  val server: Resource[IO, GrpcServer[IO]] =
    grpcConfigs.evalMap(GrpcServer.netty[IO](port, _)).flatMap { s =>
      Resource.make(s.start)(_ => s.shutdown >> s.awaitTermination).as(s)
    }

  val clientCertChainFile: File                    = TestUtils.loadCert("client.pem")
  val clientPrivateKeyFile: File                   = TestUtils.loadCert("client.key")
  val clientTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  val clientSslContext: SslContext =
    GrpcSslContexts.forClient
      .keyManager(clientCertChainFile, clientPrivateKeyFile)
      .trustManager(clientTrustedCaCerts: _*)
      .build()

  def buildChannelInterpreter(configs: List[NettyChannelConfig]): NettyChannelInterpreter =
    new NettyChannelInterpreter(
      ChannelForAddress("localhost", port),
      List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
      configs
    )

  test("server smoke test") {
    server.use(_.isShutdown).assertEquals(false)
  }

  test("TLS happy path") {
    val channelInterpreter = buildChannelInterpreter(
      List(
        NettyUsePlaintext(),
        NettyNegotiationType(NegotiationType.TLS),
        NettySslContext(clientSslContext)
      )
    )

    val client: Resource[IO, SSLService[IO]] =
      SSLService.clientFromChannel[IO](IO(channelInterpreter.build))

    (server *> client)
      .use(_.hello(Request(123)))
      .assertEquals(Response(123))
  }

  test(
    "mu-rpc client with TLS connection should " +
      "throw a io.grpc.StatusRuntimeException when no SSLContext is provided"
  ) {
    val channelInterpreter = buildChannelInterpreter(
      List(
        NettyUsePlaintext(),
        NettyNegotiationType(NegotiationType.TLS)
      )
    )

    val client: Resource[IO, SSLService[IO]] =
      SSLService.clientFromChannel[IO](IO(channelInterpreter.build))

    (server *> client)
      .use(_.hello(Request(123)))
      .intercept[StatusRuntimeException]
  }

  test(
    "mu-rpc client with SSL/TSL connection should " +
      "throw a io.grpc.StatusRuntimeException when negotiation is skipped"
  ) {

    val channelInterpreter = buildChannelInterpreter(
      List(
        NettyUsePlaintext(),
        NettySslContext(clientSslContext)
      )
    )

    val client: Resource[IO, SSLService[IO]] =
      SSLService.clientFromChannel[IO](IO(channelInterpreter.build))

    (server *> client)
      .use(_.hello(Request(123)))
      .intercept[StatusRuntimeException]
  }

}
