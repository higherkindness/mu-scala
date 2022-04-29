---
layout: docs
title: Distributed Tracing
section: guides
permalink: /guides/distributed-tracing
---

# Distributed Tracing

Mu provides an integration with [Natchez] to enable distributed tracing of
[gRPC] calls.

<a href="../img/guides/tracing.png">
  <img src="../img/guides/tracing.png" alt="Example of a distributed trace" style="width: 100%; height: auto;">
</a>

Specifically, the integration provides the following features.

## Client

* For every RPC call, the client will create a child span with the fully
  qualified name of the RPC method being called (e.g.
  `com.foo.Greeter/SayHello`)
* It will automatically add all necessary trace-related headers to RPC requests.

## Server

* The server will attempt to extract trace-related information from the request
  headers.
* It will create a span using the same naming convention as the client.
    * If the relevant headers were present, it will continue the trace that was
      started upstream, creating a child span.
    * Otherwise, it will create a root span, i.e. a new trace.

## How to use

Please, be sure you've checked the [Accessing metadata on services](accessing-metadata) first.

Let's look at how to enable tracing on the server side first.

### Server side

We'll assume the following service definition:

```scala
case class HelloRequest(name: String)
case class HelloResponse(greeting: String, happy: Boolean)

trait Greeter[F[_]] {

  def SayHello(req: HelloRequest): F[HelloResponse]

}
```

and an implementation of that definition:

```scala mdoc:silent
import mu.examples.protobuf.greeter._
import cats.Applicative
import cats.syntax.applicative._

class MyAmazingGreeter[F[_]: Applicative] extends Greeter[F] {

  def SayHello(req: HelloRequest): F[HelloResponse] =
    HelloResponse(s"Hello, ${req.name}!", happy = true).pure[F]

}
```

To use this service with tracing enabled, you need to call the
`Greeter.bindContextService[F, Span[F]]` method instead of the usual
`Greeter.bindService[F]`.

That requires an instance of `ServerContext[F, Span[F]]`, which is available in
the `higherkindness.mu.rpc.internal.tracing.implicits` object:

```scala
import higherkindness.mu.rpc.internal.context.ServerContext
import natchez.{EntryPoint, Span}

implicit def serverContext[F[_]](implicit entrypoint: EntryPoint[F]): ServerContext[F, Span[F]]
```

So, to trace our service, we need to call to `Greeter.bindContextService[F,
Span[F]]` with the import `higherkindness.mu.rpc.internal.tracing.implicits._`
in the scope and providing a [Natchez] `EntryPoint` implicitly.

#### EntryPoint

`EntryPoint[F[_]]`, as the name suggests, is the "entrypoint" into the [Natchez]
API. It's what allows Mu to do things like create root spans.

How you create an `EntryPoint` will depend on what tracing implementation you
want to use. For example, if you use `natchez-jaeger`, you might create a
`Resource` of an `EntryPoint` like this:

```scala mdoc:silent
import cats.effect.{Sync, Resource}

import natchez.EntryPoint
import natchez.jaeger.Jaeger
import io.jaegertracing.Configuration.SamplerConfiguration
import io.jaegertracing.Configuration.ReporterConfiguration

def entryPoint[F[_]: Sync]: Resource[F, EntryPoint[F]] = {
  Jaeger.entryPoint[F]("my-Mu-service") { c =>
    Sync[F].delay {
      c.withSampler(SamplerConfiguration.fromEnv)
       .withReporter(ReporterConfiguration.fromEnv)
       .getTracer
    }
  }
}
```

#### Kleisli

When you instantiate your `Greeter` implementation, you need to set its type
parameter to `Kleisli[F, Span[F], *]`. (Note: we are using [kind-projector]
syntax here, but you don't have to.)

Intuitively, this creates a service which, given the current span as input,
returns a result inside the `F` effect.

Luckily, there are instances of most of the cats-effect type classes for
`Kleisli`, all the way down to `Async`. So you should be able to substitute
`Greeter[Kleisli[F, Span[F], *]]` for `Greeter[F]` without requiring any changes
to your service implementation code.

#### Using bindContextService

Putting all this together, your server setup code will look something like this:

```scala mdoc:silent
import cats.effect._
import cats.data.Kleisli
import higherkindness.mu.rpc.server._
import natchez.Span

object TracingServer extends IOApp {

  import higherkindness.mu.rpc.internal.tracing.implicits._

  implicit val service: Greeter[Kleisli[IO, Span[IO], *]] =
    new MyAmazingGreeter[Kleisli[IO, Span[IO], *]]

  def run(args: List[String]): IO[ExitCode] =
    entryPoint[IO]
      .flatMap { implicit ep =>
        Greeter.bindContextService[IO, Span[IO]]
      }
      .flatMap { serviceDef =>
        GrpcServer.defaultServer[IO](8080, List(AddService(serviceDef)))
      }.useForever

}
```

#### Tracing your service code

If you wish, you can make use of the [Natchez] `Trace` typeclass to create child
spans:

```scala mdoc:silent
import natchez.Trace
import cats.Monad
import cats.syntax.all._

class MyTracingService[F[_]: Monad: Trace] extends Greeter[F] {

  def SayHello(req: HelloRequest): F[HelloResponse] =
    for {
      _ <- Trace[F].span("look stuff up in the database"){ Monad[F].unit }
      _ <- Trace[F].span("do some stuff with Redis"){ Monad[F].unit }
      _ <- Trace[F].span("make an HTTP call"){ Monad[F].unit }
    } yield HelloResponse(s"Hi, ${req.name}!")

}
```

### Client side

To obtain a tracing client, use `Greeter.contextClient[F, Span[F]]` instead of
`Greeter.client`.

This returns a `Greeter[Kleisli[F, Span[F], *]]`, i.e. a client which takes
the current span as input and returns a response inside the `F` effect.

Similar to the server side, there's a `ClientContext[F, Span[F]]` instance
available in the `higherkindness.mu.rpc.internal.tracing.implicits` object:

```scala
import cats.effect.Async
import higherkindness.mu.rpc.internal.context.ClientContext
import natchez.Span

implicit def clientContext[F[_]: Async]: ClientContext[F, Span[F]]
```

For example:

```scala mdoc:silent
import higherkindness.mu.rpc._

object TracingClientApp extends IOApp {

  import higherkindness.mu.rpc.internal.tracing.implicits._

  val channelFor: ChannelFor = ChannelForAddress("localhost", 8080)

  val clientRes: Resource[IO, Greeter[Kleisli[IO, Span[IO], *]]] =
    Greeter.contextClient[IO, Span[IO]](channelFor)

  def run(args: List[String]): IO[ExitCode] =
    entryPoint[IO].use { ep =>
      ep.root("this is the root span").use { currentSpan =>
        clientRes.use { client =>
          val kleisli = client.SayHello(HelloRequest("Chris"))
          for {
            resp <- kleisli.run(currentSpan)
            _    <- IO(println(s"Response: $resp"))
          } yield (ExitCode.Success)
        }
      }
    }

}
```

## Working example

To see a full working example of distributed tracing across multiple Mu
services, take a look at this project in the mu-scala-examples repo:
[higherkindness/mu-scala-examples](https://github.com/higherkindness/mu-scala-examples/tree/master/tracing).

The README explains how to run the example and inspect the resulting traces.


[gRPC]: https://grpc.io/
[kind-projector]: https://github.com/typelevel/kind-projector
[Natchez]: https://github.com/tpolecat/natchez

