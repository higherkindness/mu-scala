---
layout: docs
title: Patterns
permalink: /patterns
---

# Patterns

So far so good, we haven't seen too much code or implemented any business logic. In the previous sections we have seen some protocol definitions with Scala annotations and the generation of IDL files from the Scala definitions. Finally, in this section, we are going to see how to complete our quickstart example. We'll take a look at both sides, the server and the client.

## Server

Predictably, generating the server code is just creating an implementation for your service algebra.

First, here's our `Greeter` RPC protocol definition:

```tut:invisible
import higherkindness.mu.rpc.protocol._

object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
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

```tut:silent
import cats.effect.Async
import cats.syntax.applicative._
import higherkindness.mu.rpc.internal.task._
import monix.reactive.Observable
import service._

import scala.concurrent.ExecutionContext

class ServiceHandler[F[_]: Async](implicit EC: ExecutionContext) extends Greeter[F] {

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

That's it! We have exposed a potential implementation on the server side.

### Server Runtime

As you can see, the generic handler above requires `F` as the type parameter, which corresponds with our target `Monad` when interpreting our program. In this section, we will satisfy all the runtime requirements, in order to make our server runnable.

### Creating a runtime

Since [mu] relies on `ConcurrentEffect` from the [cats-effect library](https://github.com/typelevel/cats-effect), we'll need a runtime for executing our effects. 

We'll be using `IO` from `cats-effect`, but you can use any type that has a `ConcurrentEffect` instance.

For executing `IO` we need a `ContextShift[IO]` used for running `IO` instances and a `Timer[IO]` that is used for scheduling, let's go ahead and create them.

```tut:silent
trait CommonRuntime {

  implicit val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val timer: cats.effect.Timer[cats.effect.IO]     = cats.effect.IO.timer(EC)
  implicit val cs: cats.effect.ContextShift[cats.effect.IO] = cats.effect.IO.contextShift(EC)

}
```

As a side note, `CommonRuntime` will also be used later on for the client example program.

### Transport Implicits

For the server bootstrapping, remember to add the `mu-rpc-server` dependency to your build.

Now, we need to implicitly provide two things:

* A runtime interpreter of our `ServiceHandler` tied to a specific type. In our case, we'll use `cats.effects.IO`.
* A `ServerW` implicit evidence, compounded by:
	* RPC port where the server will bootstrap.
	* The set of configurations we want to add to our [gRPC] server, like our `Greeter` service definition. All these configurations are aggregated in a `List[GrpcConfig]`. Later on, an internal builder will build the final server based on this list. The full list of exposed settings is available in [this file](https://github.com/higherkindness/mu/blob/master/modules/server/src/main/scala/GrpcConfig.scala).

In summary, the result would be as follows:

```tut:silent
import cats.effect.IO
import higherkindness.mu.rpc.server._
import service._

object gserver {

  trait Implicits extends CommonRuntime {

    implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

  }

  object implicits extends Implicits

}
```

### Server Bootstrap

What else is needed? We just need to define a `main` method:

```tut:silent
import cats.effect.IO
import higherkindness.mu.rpc.server.GrpcServer

object RPCServer {

  import gserver.implicits._

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

The Server will bootstrap on port `8080`.

Fortunately, once all the runtime requirements are in place (**`import gserver.implicits._`**), we only have to write the previous piece of code, which primarily, should be the same in all cases (except if your target Monad is different from `cats.effects.IO`).

### Service testing

Thanks to `withServerChannel` from the package `mu.rpc.testing.servers`, you will be able to run in-memory instances of the server, which is very convenient for testing purposes. Below, a very simple property-based test for proving `Greeter.sayHello`:

```tut:silent
import cats.effect.Resource
import higherkindness.mu.rpc.testing.servers.withServerChannel
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import org.scalatest.prop.Checkers
import org.scalatest._
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import service._

class ServiceSpec extends FunSuite with Matchers with Checkers with OneInstancePerTest {

  import gserver.implicits._
    
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

```tut
run(new ServiceSpec)
```


## Client

[mu] derives a client automatically based on the protocol. This is especially useful because you can distribute it depending on the protocol/service definitions. If you change something in your protocol definition, you will get a new client for free without having to write anything.

You will need to add either `mu-rpc-client-netty` or `mu-rpc-client-okhttp` to your build.

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

```tut:silent
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

```tut:silent
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
