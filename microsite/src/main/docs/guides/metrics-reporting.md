---
layout: docs
title: Metrics Reporting
section: guides
permalink: /guides/metrics-reporting
---

# Metrics Reporting

Currently, [Mu] provides two different ways to report metrics about [gRPC]
services: `Prometheus` and `Dropwizard Metrics`. The usage is quite similar for
both.

Mu exposes the following metrics, for both servers and clients:

* **Active calls**: number of in-flight messages
* **Messages sent**: number of requests sent by the client or responses sent by
  the server (distributed by service name and method name).
* **Messages received**: number of requests received by the server or responses
  received by the client (distributed by service name and method name).
* **Timers** for header calls, total calls, and also distributed by method types
  (unary, streaming, …) and statuses (ok, canceled, …).

## Monitor Server Calls

In order to monitor the RPC calls on the server side we need two things:

* A `MetricsOps` implementation. `MetricsOps` is an algebra located in the
  `mu-rpc-service` module, which defines the necessary operations for reporting
  metrics. [Mu] provides two implementations, one for `Prometheus` and another
  one for `Dropwizard` but you can provide your own.
* A `MetricsServerInterceptor`. [Mu] provides an interceptor that receives a
  `MetricsOps` as an argument and collects server metrics.

Let's see how to register server metrics using `Prometheus` in the following
fragment.

```scala mdoc:invisible
import mu.examples.protobuf.greeter._
import cats.Applicative
import cats.syntax.applicative._

class ServiceHandler[F[_]: Applicative] extends Greeter[F] {

  override def SayHello(request: HelloRequest): F[HelloResponse] =
    HelloResponse(greeting = "Good bye!").pure[F]

}
```

```scala mdoc:silent
import mu.examples.protobuf.greeter._
import cats.effect.{IO, Resource}
import cats.effect.std.Dispatcher
import higherkindness.mu.rpc.prometheus.PrometheusMetrics
import higherkindness.mu.rpc.server._
import higherkindness.mu.rpc.server.interceptors.implicits._
import higherkindness.mu.rpc.server.metrics.MetricsServerInterceptor
import io.prometheus.client.CollectorRegistry

object InterceptingServerCalls {

  lazy val cr: CollectorRegistry = new CollectorRegistry()

  implicit val greeterServiceHandler: Greeter[IO] = new ServiceHandler[IO]

  val server: Resource[IO, GrpcServer[IO]] = for {
    metricsOps  <- Resource.eval(PrometheusMetrics.build[IO](cr, "server"))
    service     <- Greeter.bindService[IO]
    disp        <- Dispatcher[IO]
    withMetrics = service.interceptWith(MetricsServerInterceptor(metricsOps, disp))
    server      <- GrpcServer.defaultServer[IO](8080, List(AddService(withMetrics)))
  } yield server

}
```

## Monitor Client Calls

In this case, in order to intercept the client calls we need additional
configuration settings (by using `AddInterceptor`):

```scala mdoc:silent
import mu.examples.protobuf.greeter._
import cats.effect.{IO, Resource}
import cats.effect.std.Dispatcher
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.channel._
import higherkindness.mu.rpc.channel.metrics.MetricsChannelInterceptor
import io.prometheus.client.CollectorRegistry

object InterceptingClientCalls {

  lazy val cr: CollectorRegistry = new CollectorRegistry()

  val serviceClient: Resource[IO, Greeter[IO]] =
    for {
      metricsOps    <- Resource.eval(PrometheusMetrics.build[IO](cr, "client"))
      disp          <- Dispatcher[IO]
      serviceClient <- Greeter.client[IO](
        channelFor = ChannelForAddress("localhost", 8080),
        channelConfigList = List(UsePlaintext(), AddInterceptor(MetricsChannelInterceptor(metricsOps, disp))))
    } yield serviceClient

}
```

That is how we use `Prometheus` to monitor both [gRPC] ends.

## Dropwizard Metrics

The usage the same as before, but in this case we need to create a `Dropwizard`
backed `MetricsOps`:

```scala mdoc:silent
import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.dropwizard.DropWizardMetrics

val registry: MetricRegistry = new MetricRegistry()
val metricsOps = DropWizardMetrics[IO](registry)
```

To check the metrics from our server or client, `Dropwizard` exposes it through
`JMX`. You'll need the following dependency:

```
"io.dropwizard.metrics" % "metrics-jmx" % "4.1.4"
```

And to associate a JMX reporter with the metrics registry on your project,

```scala mdoc:compile-only
val jmxReporter = com.codahale.metrics.jmx.JmxReporter.forRegistry(registry)
jmxReporter.build().start()
```

## Further reading

You can see a full example in the [metrics integration with Mu] blog post.

[metrics integration with Mu]: https://www.47deg.com/blog/metrics-integration-with-mu/
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu-scala

