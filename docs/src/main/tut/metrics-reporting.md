---
layout: docs
title: Metrics Reporting
permalink: /metrics-reporting
---

# Metrics Reporting

Currently, [mu] provides two different ways to monitor [gRPC] services: `Prometheus` and `Dropwizard`. The usage is quite similar for both.

## Monitor Server Calls

In order to monitor the RPC calls on the server side we need two things:

* A `MetricsOps` implementation. `MetricsOps` is an algebra located in the `internal-core` module with the needed operations for registering metrics. [mu] provides two implementations, one for `Prometheus` and another one for `Dropwizard` but you can provide your own.
* A `MetricsServerInterceptor`. [mu] provides an interceptor that receives a `MetricsOps` as an argument and register server metrics.

Let's see how to register server metrics using `Prometheus` in the following fragment.

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
import cats.effect.IO
import higherkindness.mu.rpc.prometheus.PrometheusMetrics
import higherkindness.mu.rpc.server._
import higherkindness.mu.rpc.server.interceptors.implicits._
import higherkindness.mu.rpc.server.metrics.MetricsServerInterceptor
import io.prometheus.client.CollectorRegistry
import service._

object InterceptingServerCalls extends CommonRuntime {

  lazy val cr: CollectorRegistry = new CollectorRegistry()

  implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]
  
  val server: IO[GrpcServer[IO]] = for {
    metricsOps <- PrometheusMetrics.build[IO](cr, "server")
    service    <- Greeter.bindService[IO]
    grpcConfig = AddService(service.interceptWith(MetricsServerInterceptor(metricsOps)))
    server     <- GrpcServer.default[IO](8080, List(grpcConfig))
  } yield server

}
```

## Monitor Client Calls

In this case, in order to intercept the client calls we need additional configuration settings (by using `AddInterceptor`):

```tut:silent
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.config._
import higherkindness.mu.rpc.channel._
import higherkindness.mu.rpc.channel.metrics.MetricsChannelInterceptor
import higherkindness.mu.rpc.config.channel._
import io.prometheus.client.CollectorRegistry
import service._

object InterceptingClientCalls extends CommonRuntime {

  lazy val cr: CollectorRegistry = new CollectorRegistry()

  implicit val serviceClient: Resource[IO, Greeter[IO]] = 
    for {
      channelFor    <- Resource.liftF(ConfigForAddress[IO]("rpc.host", "rpc.port"))
      metricsOps    <- Resource.liftF(PrometheusMetrics.build[IO](cr, "client"))
      serviceClient <- Greeter.client[IO](
        channelFor = channelFor, 
        channelConfigList = List(UsePlaintext(), AddInterceptor(MetricsChannelInterceptor(metricsOps))))
    } yield serviceClient
}
```

That is how we use `Prometheus` to monitor both [gRPC] ends.

## Dropwizard Metrics

The usage the same as before, but in this case we need to create a `Dropwizard` backed `MetricsOps` 

```tut:silent
import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.dropwizard.DropWizardMetrics

val metricsOps = DropWizardMetrics[IO](new MetricRegistry)
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

