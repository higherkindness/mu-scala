---
layout: docs
title: SSL/TLS
permalink: /docs/rpc/ssl-tls
---

# SSL/TLS Encryption

> [gRPC](https://grpc.io/docs/guides/auth.html) has SSL/TLS integration and promotes the use of SSL/TLS to authenticate the server, and encrypt all the data exchanged between the client and the server. Optional mechanisms are available for clients to provide certificates for mutual authentication.

[mu-rpc] allows you to encrypt the connection between the server and the client through SSL/TLS. The main goal of using SSL is to protect your sensitive information and keeps your data secure between servers and clients.

As we mentioned in the [Quickstart](/docs/rpc/quickstart) section, we can choose and configure our client with `OkHttp` or `Netty` but if we want to encrypt our service, it's mandatory to use `Netty` because currently, [mu-rpc] only supports encryption over *Netty*.

## Requirements 

On the server and client side, we will need two files to configure the `SslContext` in `gRPC`:

* Server/Client certificate file: Small data files that digitally bind a cryptographic key to an organization’s details. This file could be generated or obtained from a third company.

* Server/Client private key file: The private key is a separate file that is used in the encryption of data sent between your server and the clients. All SSL certificates require a private key to work.

## Usage

The first step to secure our [mu-rpc] services is adding the library dependencies `mu-rpc-netty-ssl` and `mu-rpc-client-netty` in your build.

In second place, we have to move both server/client certificates and private keys to the `resources` folder.

If we haven't yet generated or obtained our own certificates, we can test using certificates found [here](https://github.com/grpc/grpc-java/tree/master/testing/src/main/resources/certs).

Thirdly, let's see a piece of code where we will explain line by line, what we are doing on the server side.

We won't cover the details regarding creation of `RPCService`, `ServerRPCService` and runtime implicits. You can find more information about these in the [Patterns](/docs/rpc/patterns) section.

```tut:invisible
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

}
```

```tut:invisible
import mu.rpc.protocol._
import monix.execution.Scheduler

object service {

  import monix.reactive.Observable

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
import cats.effect.Async
import cats.syntax.applicative._
import freestyle.free._
import mu.rpc.server.implicits._
import monix.execution.Scheduler
import monix.eval.Task
import monix.reactive.Observable
import service._

class ServiceHandler[F[_]: Async](implicit S: Scheduler) extends Greeter[F] {

  override def sayHello(request: service.HelloRequest): F[service.HelloResponse] =
    HelloResponse(reply = "Good bye!").pure

}
```

```tut:silent
import java.io.File
import java.security.cert.X509Certificate

import cats.effect.IO
import cats.effect.Effect
import mu.rpc.protocol._
import mu.rpc.server.netty.SetSslContext
import mu.rpc.server.{AddService, GrpcConfig, GrpcServer}
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

    val grpcConfigs: List[GrpcConfig] = List(
      SetSslContext(serverSslContext),
      AddService(Greeter.bindService[IO])
    )

    // Important. We have to create the server with Netty. OkHttp is not supported for the Ssl 
    // encryption in mu-rpc at this moment.

    val server: IO[GrpcServer[IO]] = GrpcServer.netty[IO](8080, grpcConfigs)

}

object implicits extends Runtime

```

Lastly, as we did before with the server side, let's see what happens on the client side.

```tut:silent
import mu.rpc.ChannelForAddress
import mu.rpc.client.OverrideAuthority
import mu.rpc.client.netty.{
  NettyChannelInterpreter,
  NettyNegotiationType,
  NettySslContext,
  NettyUsePlaintext
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
        List(
          OverrideAuthority(TestUtils.TEST_SERVER_HOST),
          NettyUsePlaintext(),
          NettyNegotiationType(NegotiationType.TLS),
          NettySslContext(clientSslContext)
        )
    )

    val muRPCServiceClient: Greeter.Client[IO] = 
    	Greeter.clientFromChannel[IO](channelInterpreter.build)

}

```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu-rpc]: https://github.com/higherkindness/mu-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier

