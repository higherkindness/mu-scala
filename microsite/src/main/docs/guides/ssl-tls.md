---
layout: docs
title: SSL/TLS
section: guides
permalink: /guides/ssl-tls
---

# SSL/TLS Encryption

From the [gRPC authentication guide](https://grpc.io/docs/guides/auth/):

> gRPC has SSL/TLS integration and promotes the use of SSL/TLS to authenticate
> the server and encrypt all the data exchanged between the client and the
> server. Optional mechanisms are available for clients to provide certificates
> for mutual authentication.

[Mu] allows you to encrypt the connection between the server and the client
through SSL/TLS. The main goal of using SSL is to protect your sensitive
information and to keep your data secure between servers and clients.

## Netty transport

Mu allows you to choose the underlying transport layer you want to use for your
gRPC servers and clients:

* For the server you can use `Netty`, or the default transport provided by the
  gRPC Java library. (In reality the default transport will also be `Netty`,
  unless you have written your own `io.grpc.ServerProvider` implementation and
  added it to the classpath.)
* For the client you can use `Netty` or `OkHttp`.

However, SSL/TLS encryption in Mu is currently only supported for servers and
clients that use the `Netty` transport.

## Requirements

On the server and client side, we will need two files to configure the
`SslContext` in `gRPC`:

* Server/Client certificate file: Small data files that digitally bind a
  cryptographic key to an organization’s details. This file could be generated
  or obtained from a third party.

* Server/Client private key file: The private key is a separate file that is
  used in the encryption of data sent between your server and the clients. All
  SSL certificates require a private key to work.

## Usage

The first step to secure our [Mu] services is to add the library dependencies
`mu-rpc-netty-ssl` and `mu-rpc-client-netty` in our build.

For the second step, we have to move both server/client certificates and private
keys to a place where they can be loaded at runtime, either from the filesystem
or the classpath. However, these files contain secrets, so they should **not**
be included in the project and committed to git.

If we haven't yet generated or obtained our own certificates, we can test using
certificates found
[here](https://github.com/grpc/grpc-java/tree/master/testing/src/main/resources/certs).

### Server side

Let's see a piece of code where we will explain line by line how to build a gRPC
server with SSL encryption enabled.

We won't cover the details of implementing the `Greeter` service or starting
the gRPC server. You can find more information about these in the [gRPC server
and client tutorial](../tutorials/grpc-server-client).

```scala mdoc:invisible
import mu.examples.protobuf.greeter._
import cats.Applicative
import cats.syntax.applicative._

class ServiceHandler[F[_]: Applicative] extends Greeter[F] {

  override def SayHello(request: HelloRequest): F[HelloResponse] =
    HelloResponse("Good bye!", happy = true).pure

}
```

```scala mdoc:silent
import java.io.File
import java.security.cert.X509Certificate

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.server.netty.SetSslContext
import higherkindness.mu.rpc.server.{AddService, GrpcConfig, GrpcServer}
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.netty.handler.ssl.{ClientAuth, SslContext, SslProvider}

object ServerExample {

  implicit val muRPCHandler: Greeter[IO] =
    new ServiceHandler[IO]

  // Load the certicate and private key files.

  val serverCertFile: File                         = TestUtils.loadCert("server1.pem")
  val serverPrivateKeyFile: File                   = TestUtils.loadCert("server1.key")
  val serverTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  // Build the SslContext, passing our server certificate, private key, and trusted certs.
  // Configure the server to use OpenSSL and require client authentication.

  val serverSslContext: SslContext =
    GrpcSslContexts
      .configure(
        GrpcSslContexts.forServer(serverCertFile, serverPrivateKeyFile),
        SslProvider.OPENSSL)
      .trustManager(serverTrustedCaCerts: _*)
      .clientAuth(ClientAuth.REQUIRE)
      .build()

  // Add the SslContext to the list of GrpConfigs.

  val grpcConfigs: Resource[IO, List[GrpcConfig]] =
     Greeter.bindService[IO]
       .map(AddService(_))
       .map(c => List(SetSslContext(serverSslContext), c))

  // Important: we have to create the server with Netty.
  // This is the only server transport that supports SSL encryption.

  val server: Resource[IO, GrpcServer[IO]] = grpcConfigs.evalMap(GrpcServer.netty[IO](8080, _))

}
```

### Client side

Similarly, let's see how to create a gRPC client with encryption and client
authentication.

```scala mdoc:silent
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.OverrideAuthority
import higherkindness.mu.rpc.channel.netty.{NettyChannelInterpreter, NettyNegotiationType, NettySslContext}
import io.grpc.netty.NegotiationType

object ClientExample {

  // Load the certicate and private key files.

  val clientCertChainFile: File                    = TestUtils.loadCert("client.pem")
  val clientPrivateKeyFile: File                   = TestUtils.loadCert("client.key")
  val clientTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

  // We have to create the SslContext for the client, like we did for the server.

  val clientSslContext: SslContext =
    GrpcSslContexts.forClient
      .keyManager(clientCertChainFile, clientPrivateKeyFile)
      .trustManager(clientTrustedCaCerts: _*)
      .build()

  // Important: the channel interpreter must be NettyChannelInterpreter.
  // We configure the channel interpreter to enable TLS and to use the SSL context we built.

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

## Further reading

For more details,
[here](https://www.47deg.com/blog/mu-rpc-securing-communications-with-mu/) you
can check a full explanation and an example about securing communications.

[Mu]: https://github.com/higherkindness/mu-scala

