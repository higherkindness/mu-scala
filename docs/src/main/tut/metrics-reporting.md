---
layout: docs
title: Metrics Reporting
permalink: /docs/rpc/metrics-reporting
---

# Metrics Reporting

Currently, [mu] provides two different ways to monitor [gRPC] services: `Prometheus` and `Dropwizard` (using the `Prometheus` extension). The usage is quite similar for both.

## Monitor Server Calls

In order to monitor the RPC calls on the server side, it's necessary to intercept them. We'll see how to do this in the next code fragment:

```tut:invisible
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

}
```

```tut:invisible
import mu.rpc.protocol._

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
import cats.~>
import cats.effect.IO
import mu.rpc.server._
import mu.rpc.server.handlers._
import mu.rpc.server.implicits._
import mu.rpc.prometheus.shared.Configuration
import mu.rpc.prometheus.server.MonitoringServerInterceptor
import io.prometheus.client.CollectorRegistry
import service._

object InterceptingServerCalls extends CommonRuntime {

  import mu.rpc.interceptors.implicits._

  lazy val cr: CollectorRegistry = new CollectorRegistry()
  lazy val monitorInterceptor = MonitoringServerInterceptor(
    Configuration.defaultBasicMetrics.withCollectorRegistry(cr)
  )

  implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

  // The Greeter service is the service defined in the Core concepts section
  val grpcConfigs: List[GrpcConfig] = List(
    AddService(Greeter.bindService[IO].interceptWith(monitorInterceptor))
  )

  val server: IO[GrpcServer[IO]]= GrpcServer.default[IO](8080, grpcConfigs)

}
```

## Monitor Client Calls

In this case, in order to intercept the client calls we need additional configuration settings (by using `AddInterceptor`):

```tut:silent
import cats.implicits._
import cats.effect.IO
import mu.rpc._
import mu.rpc.config._
import mu.rpc.client._
import mu.rpc.client.config._
import mu.rpc.client.implicits._
import monix.eval.Task
import io.grpc.ManagedChannel
import service._

import scala.util.{Failure, Success, Try}

import mu.rpc.prometheus.shared.Configuration
import mu.rpc.prometheus.client.MonitoringClientInterceptor

object InterceptingClientCalls extends CommonRuntime {

  val channelFor: ChannelFor =
    ConfigForAddress[IO]("rpc.host", "rpc.port").unsafeRunSync

  implicit val serviceClient: Greeter.Client[Task] =
    Greeter.client[Task](
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

That's using `Prometheus` to monitor both [gRPC] ends.

## Dropwizard Metrics

The usage is equivalent, however, in this case, we need to put an instance of `com.codahale.metrics.MetricRegistry` on the scene, then, using the _Dropwizard_ integration that _Prometheus_ already provides (`DropwizardExports`) you can associate it with the collector registry:

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
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier

