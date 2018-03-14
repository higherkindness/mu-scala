---
layout: docs
title: SSL/TLS
permalink: /docs/rpc/ssl-tls
---

# SSL/TLS Encryption

> [gRPC](https://grpc.io/docs/guides/auth.html) has SSL/TLS integration and promotes the use of SSL/TLS to authenticate the server, and encrypt all the data exchanged between the client and the server. Optional mechanisms are available for clients to provide certificates for mutual authentication.

[frees-rpc] allows you to encrypt the connection between the server and the client through SSL/TLS. The main goal of using SSL is to protect your sensitive information and keeps your data secure between servers and clients.

As we mentioned in the [Quickstart](/docs/rpc/quickstart) section, we can choose and configure our client with `OkHttp` or `Netty` but if we want to encrypt our service, it's mandatory to use `Netty`.

[frees-rpc] only supports encryptation over *Netty*.

## Requirements 

On the server and client side, we will need two files to configure the `SslContext` in `gRPC`:

* Server/Client certificate file: Small data files that digitally bind a cryptographic key to an organization’s details. This file could be generated or obtained from a third company.

* Server/Client private key file: The private key is a separate file that is used in the encryption of data sent between your server and the clients. All SSL certificates require a private key to work.

## Usage

The first step to do, in order to start to secure ours `freestyle-rpc` services, is use the artifacts `frees-rpc-netty-ssl` and `frees-rpc-client-netty` in our `build.sbt`.

In second place, we will have to move to our `resources` folder both server/client certificates and private keys. 

In our tests, the certificates have been extracted from [here](https://github.com/grpc/grpc-java/tree/master/testing/src/main/resources/certs). It could be a good point to start to check that our application works meanwhile we generate or obtain our own certificates.

Thirdly, let's see a piece of code where we will explain line by line, what we are doing on the server side.

We are going to ignore the explanations related to create the `RPCService`, `ServerRPCService` and runtime implicits. If you have any doubt about it, please, take a look at the [Patterns](/docs/rpc/patterns) section.

```tut:silent
import java.io.File
import java.security.cert.X509Certificate

import cats.effect.Effect
import freestyle.rpc.common._
import freestyle.rpc.protocol._
import freestyle.rpc.server.netty.SetSslContext
import freestyle.rpc.server.{AddService, GrpcConfig, ServerW}
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.netty.handler.ssl.{ClientAuth, SslContext, SslProvider}

object service {

    @service
    trait RPCService[F[_]] {
      @rpc(Avro) def unary(a: A): F[C]
    }

}

object handlers {

    class ServerRPCService[F[_]: Effect] extends RPCService[F] {
    	def unary(a: A): F[C] = Effect[F].delay(c1)
    }

}

trait Runtime {

	import service._
    import handlers._

    implicit val freesRPCHandler: ServerRPCService[ConcurrentMonad] =
      new ServerRPCService[ConcurrentMonad]

    // First of all, we have to load the certs into files. These files has to be locally in our
    // module in the resources folder.

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

    // Adding to the GrpConfig our SslContext with SetSslContext, when we will create implicitly our 
    // server with ServerW.netty, our server will require SSL encryption in the client side.

    val grpcConfigs: List[GrpcConfig] = List(
      SetSslContext(serverSslContext),
      AddService(RPCService.bindService[ConcurrentMonad])
    )

    // Important. We have to create the server with Netty. OkHttp is not supported for the Ssl 
    // encryption in frees-rpc.

    implicit val serverW: ServerW = ServerW.netty(SC.port, grpcConfigs)

}

object implicits extends Runtime

```


Lastly, as we did before with the server side, let's see what happens on the client side.

```tut:silent

import freestyle.rpc.client.OverrideAuthority
import freestyle.rpc.client.netty.{
  NettyChannelInterpreter,
  NettyNegotiationType,
  NettySslContext,
  NettyUsePlaintext
}

object client {

    // First of all, we have to load the certs into files.

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

object MainApp {

	import client._

	// Important, the channel interpreter have to be NettyChannelInterpreter.

	// In this case, we are creating the channel interpreter with a specifics ManagedChannelConfig
	// These configs allow us encrypted the connection with the server.

	val channelInterpreter: NettyChannelInterpreter = new NettyChannelInterpreter(
  		ChannelForAddress("localhost", "8080"),
        List(
          OverrideAuthority(TestUtils.TEST_SERVER_HOST),
          NettyUsePlaintext(false),
          NettyNegotiationType(NegotiationType.TLS),
          NettySslContext(clientSslContext)
        )
    )

    val freesRPCServiceClient: RPCService.Client[ConcurrentMonad] = 
    	RPCService.clientFromChannel[ConcurrentMonad](channelInterpreter.build)

}

```