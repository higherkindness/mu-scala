---
layout: docs
title: SSL/TLS
permalink: /ssl-tls
---

# SSL/TLS Encryption

> [gRPC](https://grpc.io/docs/guides/auth.html) has SSL/TLS integration and promotes the use of SSL/TLS to authenticate the server and encrypt all the data exchanged between the client and the server. Optional mechanisms are available for clients to provide certificates for mutual authentication.

[mu] allows you to encrypt the connection between the server and the client through SSL/TLS. The main goal of using SSL is to protect your sensitive information and to keep your data secure between servers and clients.

As we mentioned in the [Main](/mu/scala/) section, we can choose to configure our client with `OkHttp` or `Netty` but if we want to encrypt our service, it's mandatory to use `Netty`. Currently, [mu] only supports encryption over *Netty*.

## Requirements 

On the server and client side, we will need two files to configure the `SslContext` in `gRPC`:

* Server/Client certificate file: Small data files that digitally bind a cryptographic key to an organization’s details. This file could be generated or obtained from a third company.

* Server/Client private key file: The private key is a separate file that is used in the encryption of data sent between your server and the clients. All SSL certificates require a private key to work.

## Usage

The first step to secure our [mu] services is to add the library dependencies `mu-rpc-netty-ssl` and `mu-rpc-netty` in our build.

For the second step, we have to move both server/client certificates and private keys to the `resources` folder.

If we haven't yet generated or obtained our own certificates, we can test using certificates found [here](https://github.com/grpc/grpc-java/tree/master/testing/src/main/resources/certs).

Thirdly, let's see a piece of code where we will explain line by line, what we are doing on the server side.

We won't cover the details regarding creation of `RPCService`, `ServerRPCService` and runtime implicits. You can find more information about these in the [Patterns](patterns) section.

```tut:invisible
trait CommonRuntime {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val timer: cats.effect.Timer[cats.effect.IO]     = cats.effect.IO.timer(EC)
  implicit val cs: cats.effect.ContextShift[cats.effect.IO] = cats.effect.IO.contextShift(EC)

}
```

```tut:invisible
import higherkindness.mu.rpc.protocol._

object service {

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service(Protobuf)
  trait Greeter[F[_]] {
    def sayHello(request: HelloRequest): F[HelloResponse]
  }
}
```

```tut:invisible
import cats.Applicative
import cats.syntax.applicative._
import service._

class ServiceHandler[F[_]: Applicative] extends Greeter[F] {

  override def sayHello(request: service.HelloRequest): F[service.HelloResponse] =
    HelloResponse(reply = "Good bye!").pure

}
```

```tut:silent
import java.io.File
import java.security.cert.X509Certificate

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.server.netty.SetSslContext
import higherkindness.mu.rpc.server.{AddService, GrpcConfig, GrpcServer}
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.netty.handler.ssl.{ClientAuth, SslContext, SslProvider}

trait Runtime extends CommonRuntime {

  implicit val muRPCHandler: ServiceHandler[IO] =
    new ServiceHandler[IO]

  // First of all, we have to load the certs into files. These files have to be placed in the resources folder.

  val serverCertFile: File                         = TestUtils.loadCert("server1.pem")
  val serverPrivateKeyFile: File                   = TestUtils.loadCert("server1.key")
  val serverTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  // We have to build the SslContext passing our server certificates, configuring the OpenSSL
  // and requiring the client auth.

  val serverSslContext: SslContext =
    GrpcSslContexts
      .configure(
        GrpcSslContexts.forServer(serverCertFile, serverPrivateKeyFile),
        SslProvider.OPENSSL)
      .trustManager(serverTrustedCaCerts: _*)
      .clientAuth(ClientAuth.REQUIRE)
      .build()

  // Adding to the GrpConfig list the SslContext:

  val grpcConfigs: IO[List[GrpcConfig]] =
     Greeter.bindService[IO]
       .map(AddService)
       .map(c => List(SetSslContext(serverSslContext), c))

  // Important. We have to create the server with Netty. OkHttp is not supported for the Ssl
  // encryption in mu-rpc at this moment.

  val server: IO[GrpcServer[IO]] = grpcConfigs.flatMap(GrpcServer.netty[IO](8080, _))

}

object implicits extends Runtime
```

Lastly, as we did before with the server side, let's see what happens on the client side.

```tut:silent
import cats.syntax.either._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.OverrideAuthority
import higherkindness.mu.rpc.channel.netty.{
  NettyChannelInterpreter,
  NettyNegotiationType,
  NettySslContext
}
import io.grpc.netty.NegotiationType

object client {

  // First of all, we have to load the certs files.

  val clientCertChainFile: File                    = TestUtils.loadCert("client.pem")
  val clientPrivateKeyFile: File                   = TestUtils.loadCert("client.key")
  val clientTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  // We have to create the SslContext for the client like as we did in the server.

  val clientSslContext: SslContext =
    GrpcSslContexts.forClient
      .keyManager(clientCertChainFile, clientPrivateKeyFile)
      .trustManager(clientTrustedCaCerts: _*)
      .build()
}

object MainApp extends CommonRuntime {

  import client._

  // Important, the channel interpreter have to be NettyChannelInterpreter.

  // In this case, we are creating the channel interpreter with a specific ManagedChannelConfig
  // These configs allow us to encrypt the connection with the server.

  val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
    ChannelForAddress("localhost", 8080),
    List(OverrideAuthority(TestUtils.TEST_SERVER_HOST)),
    List(
      NettyNegotiationType(NegotiationType.TLS),
      NettySslContext(clientSslContext)
    )
  )

  val muRPCServiceClient: Resource[IO, Greeter[IO]] =
    Greeter.clientFromChannel[IO](IO(channelInterpreter.build))

}
```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/47deg/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier

