---
layout: docs
title: Metrics Reporting
permalink: /metrics-reporting
---

# Metrics Reporting

Currently, [mu] provides two different ways to monitor [gRPC] services: `Prometheus` and `Dropwizard` (using the `Prometheus` extension). The usage is quite similar for both.

## Monitor Server Calls

In order to monitor the RPC calls on the server side, it's necessary to intercept them. We'll see how to do this in the next code fragment:

```tut:invisible
trait CommonRuntime {

  implicit val EC: scala.concurrent.ExecutionContext =
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
import higherkindness.mu.rpc.server._
import higherkindness.mu.rpc.prometheus.shared.Configuration
import higherkindness.mu.rpc.prometheus.server.MonitoringServerInterceptor
import io.prometheus.client.CollectorRegistry
import service._

object InterceptingServerCalls extends CommonRuntime {

  import higherkindness.mu.rpc.interceptors.implicits._

  lazy val cr: CollectorRegistry = new CollectorRegistry()
  lazy val monitorInterceptor = MonitoringServerInterceptor(
    Configuration.defaultBasicMetrics.withCollectorRegistry(cr)
  )

  implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

  // The Greeter service is the service defined in the Core concepts section
  val grpcConfigs: List[GrpcConfig] = List(
    AddService(Greeter.bindService[IO].interceptWith(monitorInterceptor))
  )

  val server: IO[GrpcServer[IO]] = GrpcServer.default[IO](8080, grpcConfigs)

}
```

## Monitor Client Calls

In this case, in order to intercept the client calls we need additional configuration settings (by using `AddInterceptor`):

```tut:silent
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.config._
import higherkindness.mu.rpc.client._
import higherkindness.mu.rpc.client.config._
import service._

import higherkindness.mu.rpc.prometheus.shared.Configuration
import higherkindness.mu.rpc.prometheus.client.MonitoringClientInterceptor

object InterceptingClientCalls extends CommonRuntime {

  val channelFor: ChannelFor =
    ConfigForAddress[IO]("rpc.host", "rpc.port").unsafeRunSync

  implicit val serviceClient: Resource[IO, Greeter[IO]] =
    Greeter.client[IO](
      channelFor = channelFor,
      channelConfigList = List(
        UsePlaintext(),
        AddInterceptor(
          MonitoringClientInterceptor(
            Configuration.defaultBasicMetrics
          )
        )
      )
    )
}
```

That is how we use `Prometheus` to monitor both [gRPC] ends.

## Dropwizard Metrics

The usage the same as before, but in this case we need to put an instance of `com.codahale.metrics.MetricRegistry` in our code. Then using the _Dropwizard_ integration that _Prometheus_ already provides (`DropwizardExports`) we can associate it with the collector registry:

```tut:silent
import com.codahale.metrics.MetricRegistry
import io.prometheus.client.dropwizard.DropwizardExports

val metrics: MetricRegistry      = new MetricRegistry
val configuration: Configuration = Configuration.defaultBasicMetrics
configuration.collectorRegistry.register(new DropwizardExports(metrics))
```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier

