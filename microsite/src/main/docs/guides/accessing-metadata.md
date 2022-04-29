---
layout: docs
title: Accessing metadata on services
section: guides
permalink: /guides/accessing-metadata
---

# Context on services

Mu provides a way to create contexts available in the client and server. Specifically, it offers the following features.

## Client

* For every RPC call, you need to create an initial context that will be passed to the client
* The client will have the ability to operate and transform that context, which will be sent to the server in the headers.

## Server

* The server will have the ability to extract the context information from the request headers and use them.

## How to use

Let's assume the following service definition:

```scala
case class HelloRequest(name: String)
case class HelloResponse(greeting: String, happy: Boolean)

trait Greeter[F[_]] {

  def SayHello(req: HelloRequest): F[HelloResponse]

}
```

Let's look at enabling the context on the client-side first.

### Client side

Ordinarily, if you don't want to use this feature, you would create a cats-effect
`Resource` of an RPC client using the auto-generated `Greeter.client` method:

```scala mdoc:silent
import mu.examples.protobuf.greeter._
import cats.effect._
import higherkindness.mu.rpc.{ChannelFor, ChannelForAddress}

object OrdinaryClientApp extends IOApp {

  val channelFor: ChannelFor = ChannelForAddress("localhost", 8080)

  val clientRes: Resource[IO, Greeter[IO]] =
    Greeter.client[IO](channelFor)

  def run(args: List[String]): IO[ExitCode] =
    clientRes.use { client =>
      for {
        resp <- client.SayHello(HelloRequest("Chris"))
        _    <- IO(println(s"Response: $resp"))
      } yield (ExitCode.Success)
    }
}
```

To obtain a client with the context available, use `Greeter.contextClient[F, C]`
instead of `Greeter.client`.

This returns a `Greeter[Kleisli[F, C, *]]`, i.e. a client which takes an
arbitrary `C` as input and returns a response inside the `F` effect.

This method requires an implicit instance in scope, specifically a
`ClientContext[F, C]`:

```scala
import cats.effect.Resource
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}

final case class ClientContextMetaData[C](context: C, metadata: Metadata)

trait ClientContext[F[_], C] {

  def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      current: C
  ): Resource[F, ClientContextMetaData[C]]

}
```

A `ClientContext` is an algebra that will take different information from the
current call and the initial context (`current`) and generates a transformed
context and an `io.grpc.Metadata`. The metadata is the information that will
travel through the wire in the requests.

There's a helper method in the companion object for generating a `ClientContext`
instance from a function:

```scala
def impl[F[_], C](f: (C, Metadata) => F[Unit]): ClientContext[F, C]
```

For example, suppose we want to pass a 'tag' via the metadata:

```scala mdoc:silent
import cats.data.Kleisli
import io.grpc.Metadata
import higherkindness.mu.rpc.internal.context.ClientContext

object TaggingClientApp extends IOApp {

  val channelFor: ChannelFor = ChannelForAddress("localhost", 8080)

  val key: Metadata.Key[String] = Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER)

  implicit val cc: ClientContext[IO, String] =
    ClientContext.impl[IO, String]((tag, md) => IO(md.put(key, tag)))

  val clientRes: Resource[IO, Greeter[Kleisli[IO, String, *]]] =
    Greeter.contextClient[IO, String](channelFor)

  def run(args: List[String]): IO[ExitCode] =
    clientRes.use { client =>
      val kleisli = client.SayHello(HelloRequest("Chris"))
      for {
        resp <- kleisli.run("my-tag")
        _    <- IO(println(s"Response: $resp"))
      } yield (ExitCode.Success)
    }

}
```

### Server side

For the server, as usual, we need an implementation of the service (shown
below):

```scala mdoc:silent
import cats.Applicative
import cats.syntax.applicative._

class MyAmazingGreeter[F[_]: Applicative] extends Greeter[F] {

  def SayHello(req: HelloRequest): F[HelloResponse] =
    HelloResponse(s"Hello, ${req.name}!", happy = true).pure[F]

}
```

In general, if you were not using context, you would need to create a gRPC
service definition using the auto-generated `Greeter.bindService` method,
specifying your effect monad of choice:

```scala mdoc:silent
import cats.effect.{IO, IOApp, ExitCode}
import higherkindness.mu.rpc.server.{GrpcServer, AddService}

object OrdinaryServer extends IOApp {

  implicit val service: Greeter[IO] = new MyAmazingGreeter[IO]

  def run(args: List[String]): IO[ExitCode] = (for {
    serviceDef <- Greeter.bindService[IO]
    _          <- GrpcServer.defaultServer[IO](8080, List(AddService(serviceDef)))
  } yield ()).useForever

}
```

To use the same service with context enabled, you need to call the
`Greeter.bindContextService` method instead.

`bindContextService[F[_], C]` differs from `bindService[F[_]]` in two ways,
which we will explain below.

1. It takes a `Greeter` as an implicit argument, but instead of a `Greeter[F]`
   it requires a `Greeter[Kleisli[F, C, *]]`.
2. It expects an implicit instance of `ServerContext[F, C]` in the scope.

A `ServerContext[F, C]` is an algebra that specifies how to build a context of
type `C` from the metadata.

```scala
import cats.effect._
import io.grpc.{Metadata, MethodDescriptor}

trait ServerContext[F[_], C] {

  def apply[Req, Res](
    descriptor: MethodDescriptor[Req, Res],
    metadata: Metadata
  ): Resource[F, C]

}
```

Like in the case of the client, we have a helper method in the companion object
that makes it easier to build instances of `ServerContext`:

```scala
def impl[F[_], C](f: Metadata => F[C]): ServerContext[F, C]
```

Then, to get access to the context in the service, we can implement the service
using the `Kleisli` as the *F-type*:

```scala mdoc:silent
import cats.Applicative
import cats.syntax.applicative._

class MyAmazingContextGreeter[F[_]: Applicative] extends Greeter[Kleisli[F, String, *]] {

  def SayHello(req: HelloRequest): Kleisli[F, String, HelloResponse] = Kleisli { tag =>
    // You can use `tag` here
    HelloResponse(s"Hello, ${req.name}! You sent me tag '$tag'", happy = true).pure[F]
  }

}
```

#### Using bindContextService

Putting all this together, your server setup code will look something like this:

```scala mdoc:silent
import cats.data.Kleisli
import higherkindness.mu.rpc.internal.context.ServerContext
import io.grpc.Metadata

object TaggingServer extends IOApp {

  implicit val service: Greeter[Kleisli[IO, String, *]] = new MyAmazingContextGreeter[IO]

  val key: Metadata.Key[String] = Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER)

  implicit val sc: ServerContext[IO, String] =
    ServerContext.impl[IO, String](md => IO(md.get(key)))

  def run(args: List[String]): IO[ExitCode] = (for {
    serviceDef <- Greeter.bindContextService[IO, String]
    _          <- GrpcServer.defaultServer[IO](8080, List(AddService(serviceDef)))
  } yield ()).useForever

}
```
