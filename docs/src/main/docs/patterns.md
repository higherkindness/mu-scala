---
layout: docs
title: Patterns
permalink: /patterns
---

# Patterns

So far so good, we haven't seen too much code or implemented any business logic. In the previous sections we have seen some protocol definitions with Scala annotations and the generation of these definitions from IDL files. Finally, in this section, we are going to see how to complete our quickstart example. We'll take a look at both sides, the server and the client.

## Server

Predictably, generating the server code is just creating an implementation for your service algebra.

First, here's our `Greeter` RPC protocol definition:

```scala mdoc:silent
import higherkindness.mu.rpc.protocol._

object service {

  import monix.reactive.Observable

  case class HelloRequest(greeting: String)

  case class HelloResponse(reply: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * @param request Client request.
     * @return Server response.
     */
    def sayHello(request: HelloRequest): F[HelloResponse]

    /**
     * @param request Single client request.
     * @return Stream of server responses.
     */
    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    /**
     * @param request Stream of client requests.
     * @return Single server response.
     */
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    /**
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]

  }

}
```

Next, our dummy `Greeter` server implementation:

```scala mdoc:silent
import cats.effect.Async
import cats.syntax.applicative._
import monix.execution.Scheduler
import monix.reactive.Observable
import service._

import scala.concurrent.ExecutionContext

class ServiceHandler[F[_]: Async](implicit S: Scheduler) extends Greeter[F] {

  private[this] val dummyObservableResponse: Observable[HelloResponse] =
    Observable.fromIterable(1 to 5 map (i => HelloResponse(s"Reply $i")))

  override def sayHello(request: HelloRequest): F[HelloResponse] =
    HelloResponse(reply = "Good bye!").pure[F]

  override def lotsOfReplies(request: HelloRequest): Observable[HelloResponse] =
    dummyObservableResponse

  override def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse] =
    request
      .foldLeftL((0, HelloResponse(""))) {
        case ((i, response), currentRequest) =>
          val currentReply: String = response.reply
          (
            i + 1,
            response.copy(
              reply = s"$currentReply\nRequest ${currentRequest.greeting} -> Response: Reply $i"))
      }
      .map(_._2)
      .toAsync[F]

  override def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse] =
    request
      .flatMap { request: HelloRequest =>
        println(s"Saving $request...")
        dummyObservableResponse
      }
      .onErrorHandle { e =>
        throw e
      }
}
```

As you can see, we need a `monix.execution.Scheduler` implicit. This is needed in order to resolve the `cats.effect` instances for `Monix`.

That's it! We have exposed a potential implementation on the server side.

### Server Runtime

As you can see, the generic handler above requires `F` as the type parameter, which corresponds with our target `Monad` when interpreting our program. In this section, we will satisfy all the runtime requirements, in order to make our server runnable.

### Creating a runtime

Since [Mu] relies on `ConcurrentEffect` from the [cats-effect library](https://github.com/typelevel/cats-effect), we'll need a runtime for executing our effects. 

We'll be using `IO` from `cats-effect`, but you can use any type that has a `ConcurrentEffect` instance.

For executing `IO` we need a `ContextShift[IO]` used for running `IO` instances and a `Timer[IO]` that is used for scheduling, let's go ahead and create them.

*Note:* You'd need an implicit `monix.execution.Scheduler` in the case you're using Monix observables.

```scala mdoc:silent
trait CommonRuntime {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val S: monix.execution.Scheduler = monix.execution.Scheduler.Implicits.global

  implicit val timer: cats.effect.Timer[cats.effect.IO]     = cats.effect.IO.timer(EC)
  implicit val cs: cats.effect.ContextShift[cats.effect.IO] = cats.effect.IO.contextShift(EC)

}
```

As a side note, `CommonRuntime` will also be used later on for the client example program.

### Server Bootstrap

For the server bootstrapping, remember to add the `mu-rpc-server` dependency to your build.

Now, we need to implicitly provide a runtime interpreter of our `ServiceHandler` tied to a specific type. In our case, we'll use `cats.effects.IO`.

We also need to explicitly provide:
	* RPC port where the server will bootstrap.
	* The set of configurations we want to add to our [gRPC] server, like our `Greeter` service definition. All these configurations are aggregated in a `List[GrpcConfig]`. The full list of exposed settings is available in [this file](https://github.com/higherkindness/mu/blob/master/modules/server/src/main/scala/GrpcConfig.scala).

What else is needed? We just need to define a `main` method:

```scala mdoc:silent
import cats.effect.IO
import higherkindness.mu.rpc.server._

object RPCServer extends CommonRuntime {

  implicit val greeterServiceHandler: Greeter[IO] = new ServiceHandler[IO]

  def main(args: Array[String]): Unit = {

    val runServer = for {
      grpcConfig <- Greeter.bindService[IO]
      server     <- GrpcServer.default[IO](8080, List(AddService(grpcConfig)))
      runServer  <- GrpcServer.server[IO](server)
    } yield runServer

    runServer.unsafeRunSync()
  }

}
```

The server will run on port `8080`.

### Service testing

Thanks to `withServerChannel` from the package `mu.rpc.testing.servers`, you will be able to run in-memory instances of the server, which is very convenient for testing purposes. Below, a very simple property-based test for proving `Greeter.sayHello`:

```scala mdoc:silent
import cats.effect.Resource
import higherkindness.mu.rpc.testing.servers.withServerChannel
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import org.scalatestplus.scalacheck.Checkers
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import service._

class ServiceSpec extends AnyFunSuite with Matchers with Checkers with CommonRuntime {

  implicit val greeterServiceHandler: Greeter[IO] = new ServiceHandler[IO]

  def withClient[Client, A](
      serviceDef: IO[ServerServiceDefinition],
      resourceBuilder: IO[ManagedChannel] => Resource[IO, Client])(
      f: Client => A): A =
    withServerChannel(serviceDef)
      .flatMap(sc => resourceBuilder(IO(sc.channel)))
      .use(client => IO(f(client)))
      .unsafeRunSync()

  def sayHelloTest(requestGen: Gen[HelloRequest], expected: HelloResponse): Assertion =
    withClient(
      Greeter.bindService[IO],
      Greeter.clientFromChannel[IO](_)) { client =>
        check {
          forAll(requestGen) { request =>
            client.sayHello(request).unsafeRunSync() == expected
          }
        }
      }

  val requestGen: Gen[HelloRequest] = Gen.alphaStr map HelloRequest

  test("Get a valid response when a proper request is passed") {
    sayHelloTest(requestGen, HelloResponse(reply = "Good bye!"))
  }

}
```

`Greeter.bindService` is an auto-derived method which creates, behind the scenes, the binding service for [gRPC]. It requires `F[_]` as the type parameter, the target/concurrent monad, in our example: `cats.effects.IO`.

Running the test:

```scala mdoc
nocolor.run(new ServiceSpec)
```


## Client

[Mu] derives a client automatically based on the protocol. This is especially useful because you can distribute it depending on the protocol/service definitions. If you change something in your protocol definition, you will get a new client for free without having to write anything.

You will need to add either `mu-rpc-netty` or `mu-rpc-okhttp` to your build.

### Client Runtime

Similarly in this section, just like we saw for the server, we are defining all the client runtime configurations needed for communication with the server.

### Creating a runtime

In our example, we are going to use the same runtime described for the server. Remember we're relying on the `ConcurrentEffect`, so you can use any type that has a `ConcurrentEffect` instance. 

### Transport Implicits

First, we need to configure how the client will reach the server in terms of the transport layer. There are two supported methods:

* By Address (host/port): enables us to create a channel with the target's address and port number.
* By Target: we can create a channel with a target string, which can be either a valid [NameResolver](https://grpc.io/grpc-java/javadoc/io/grpc/NameResolver.html)-compliant URI or an authority string.

Additionally, we can add more optional configurations that can be used when the connection occurs. All the options are available [here](https://github.com/higherkindness/mu/blob/48b8578cbafc59dc59e61e93c13fdb4709b7e540/modules/client/src/main/scala/ManagedChannelConfig.scala).

Given the transport settings and a list of optional configurations, we can create the [ManagedChannel.html](https://grpc.io/grpc-java/javadoc/io/grpc/ManagedChannel.html) object, using the `ManagedChannelInterpreter` builder.

As we will see shortly in our example, we are going relay on the default behaviour passing directly the `ChannelFor` to the client builder.

So, taking into account all of that, how would our code look?

```scala mdoc:silent
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.config._
import higherkindness.mu.rpc.config.channel._
import service._

object gclient {

  trait Implicits extends CommonRuntime {

    val channelFor: ChannelFor =
      ConfigForAddress[IO]("rpc.host", "rpc.port").unsafeRunSync

    implicit val serviceClient: Resource[IO, Greeter[IO]] =
      Greeter.client[IO](channelFor)
  }

  object implicits extends Implicits
}
```

**Notes**:

* In the code above, `host` and `port` would be read from an application configuration file.
* To be able to use the `ConfigForAddress` helper, you need to add the `mu-config` dependency to your build.

### Client Program

Once we have our runtime configuration defined as above, everything gets easier on the client side. This is an example of a client application, following our dummy quickstart:

```scala mdoc:silent
import cats.effect.IO
import service._
import gclient.implicits._

object RPCDemoApp {

  def main(args: Array[String]): Unit = {

    val hello = serviceClient.use(_.sayHello(HelloRequest("foo"))).flatMap { result =>
      IO(println(s"Result = $result"))
    }

    hello.unsafeRunSync()
  }

}
```
## More

You can check a full explanation about defining messages and services [here](https://www.47deg.com/blog/mu-rpc-defining-messages-and-services/) or some examples [here](https://github.com/higherkindness/mu/tree/master/modules/examples).


[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/47deg/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
