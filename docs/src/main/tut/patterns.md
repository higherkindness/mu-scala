---
layout: docs
title: Patterns
permalink: /docs/rpc/patterns
---

# Patterns

So far so good, not too much code, no business logic, in the previous sections we have seen some protocol definitions with Scala annotations and the generation of IDL files from the Scala definitions. Conversely, in this section, we are going to see how to complete our quickstart example. We'll take a look at both sides, the server and the client.

## Server

Predictably, generating the server code is just implementing a service [Handler](http://frees.io/docs/core/interpreters/).

First of all, our `Greeter` RPC protocol definition:

```tut:invisible
import freestyle.rpc.protocol._

object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service
  trait Greeter[F[_]] {

    /**
     * @param request Client request.
     * @return Server response.
     */
    @rpc(Protobuf)
    def sayHello(request: HelloRequest): F[HelloResponse]

    /**
     * @param request Single client request.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    /**
     * @param request Stream of client requests.
     * @return Single server response.
     */
    @rpc(Protobuf)
    @stream[RequestStreaming.type]
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    /**
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]

  }

}
```

Next, our dummy `Greeter` server implementation:

```tut:silent
import cats.effect.Async
import cats.syntax.applicative._
import freestyle.free._
import freestyle.rpc.server.implicits._
import monix.execution.Scheduler
import monix.eval.Task
import monix.reactive.Observable
import service._

class ServiceHandler[F[_]: Async](implicit S: Scheduler) extends Greeter[F] {

  private[this] val dummyObservableResponse: Observable[HelloResponse] =
    Observable.fromIterable(1 to 5 map (i => HelloResponse(s"Reply $i")))

  override def sayHello(request: HelloRequest): F[HelloResponse] =
    HelloResponse(reply = "Good bye!").pure

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
      .to[F]

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

That's all. We have exposed a potential implementation on the server side.

### Server Runtime

As you can see, the generic handler above requires `F` as the type parameter, which corresponds with our target `Monad` when interpreting our program. In this section, we will satisfy all the runtime requirements, in order to make our server runnable.

### Execution Context

In [frees-rpc] programs, we'll at least need an implicit evidence related to the [Monix] Execution Context: `monix.execution.Scheduler`.

> The `monix.execution.Scheduler` is inspired by `ReactiveX`, being an enhanced Scala `ExecutionContext` and also a replacement for Java’s `ScheduledExecutorService`, but also for Javascript’s `setTimeout`.

```tut:silent
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

}
```

As a side note, `CommonRuntime` will also be used later on for the client example program.

### Runtime Implicits

For the server bootstrapping, remember adding `frees-rpc-server` dependency to your build.

Now, we need to implicitly provide two things:

* A runtime interpreter of our `ServiceHandler` tied to a specific type. In our case, we'll use `cats.effects.IO`.
* A `ServerW` implicit evidence, compounded by:
	* RPC port where the server will bootstrap.
	* The set of configurations we want to add to our [gRPC] server, like our `Greeter` service definition. All these configurations are aggregated in a `List[GrpcConfig]`. Later on, an internal builder will build the final server based on this list. The full list of exposed settings is available in [this file](https://github.com/frees-io/freestyle-rpc/blob/master/modules/server/src/main/scala/GrpcConfig.scala).

In summary, the result would be as follows:

```tut:silent
import cats.~>
import cats.effect.IO
import freestyle.rpc.server._
import freestyle.rpc.server.handlers._
import freestyle.rpc.server.implicits._
import freestyle.async.catsEffect.implicits._
import service._

object gserver {

  trait Implicits extends CommonRuntime {

    implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(Greeter.bindService[IO])
    )

    implicit val serverW: ServerW = ServerW.default(8080, grpcConfigs)
  }

  object implicits extends Implicits

}
```

Here are a few additional notes related to the previous snippet of code:

* The Server will bootstrap on port `8080`.
* `Greeter.bindService` is an auto-derived method which creates, behind the scenes, the binding service for [gRPC]. It requires `F[_]` as the type parameter, the target/concurrent monad, in our example: `cats.effects.IO`.

### Server Bootstrap

What else is needed? We just need to define a `main` method:

```tut:silent
import cats.effect.IO
import cats.effect.IO._
import freestyle.rpc.server.GrpcServer
import freestyle.rpc.server.implicits._

object RPCServer {

  import gserver.implicits._

  def main(args: Array[String]): Unit =
    server[IO].unsafeRunSync()

}
```

Fortunately, once all the runtime requirements are in place (**`import gserver.implicits._`**), we only have to write the previous piece of code, which primarily, should be the same in all cases (except if your target Monad is different from `cats.effects.IO`).

### Service testing

Thanks to `withServerChannel` from the package `freestyle.rpc.testing.servers`, you will be able to run in-memory instances of the server, which is very convenient for testing purposes. Below, a very simple property-based test for proving `Greeter.sayHello`:

```tut:silent
import freestyle.rpc.testing.servers.withServerChannel
import org.scalatest.prop.Checkers
import org.scalatest._
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import service._

class ServiceSpec extends FunSuite with Matchers with Checkers with OneInstancePerTest {

  import gserver.implicits._
  
  def sayHelloTest(requestGen: Gen[HelloRequest], expected: HelloResponse): Assertion =
    withServerChannel(Greeter.bindService[IO]) { sc =>
        val client = Greeter.clientFromChannel[IO](sc.channel)
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

Running the test:

```tut
run(new ServiceSpec)
```


## Client

[frees-rpc] derives a client automatically based on the protocol. This is especially useful because you can distribute it depending on the protocol/service definitions. If you change something in your protocol definition, you will get a new client for free without having to write anything.

You will need to add either `frees-rpc-client-netty` or `frees-rpc-client-okhttp` to your build.

### Client Runtime

Similarly in this section, as we saw for the server case, we are defining all the client runtime configurations needed for communication with the server.

### Execution Context

In our example, we are going to use the same Execution Context described for the Server. However, for the sake of observing a slightly different runtime configuration, our client will be interpreting to `monix.eval.Task`. Hence, in this case, we would only need the `monix.execution.Scheduler` implicit evidence.

We are going to interpret to `monix.eval.Task`, however, behind the scenes, we will use the [cats-effect] `IO` monad as an abstraction. Concretely, Freestyle has an integration with `cats-effect` that is included transitively in the classpath through `frees-async-cats-effect` dependency.

### Runtime Implicits

First of all, we need to configure how the client will reach the server in terms of the transport layer. There are two supported methods:

* By Address (host/port): brings the ability to create a channel with the target's address and port number.
* By Target: it can create a channel with a target string, which can be either a valid [NameResolver](https://grpc.io/grpc-java/javadoc/io/grpc/NameResolver.html)-compliant URI or an authority string.

Additionally, we can add more optional configurations that can be used when the connection is occurring. All the options are available [here](https://github.com/frees-io/freestyle-rpc/blob/6b0e926a5a14fbe3d9282e8c78340f2d9a0421f3/rpc/src/main/scala/client/ChannelConfig.scala#L33-L46). As we will see shortly in our example, we are going to skip the negotiation (`UsePlaintext()`).

Given the transport settings and a list of optional configurations, we can create the [ManagedChannel.html](https://grpc.io/grpc-java/javadoc/io/grpc/ManagedChannel.html) object, using the `ManagedChannelInterpreter` builder.

So, taking into account all we have just said, how would our code look?

```tut:silent
import cats.implicits._
import cats.effect.IO
import freestyle.free.config.implicits._
import freestyle.async.catsEffect.implicits._
import freestyle.rpc._
import freestyle.rpc.client._
import freestyle.rpc.client.config._
import freestyle.rpc.client.implicits._
import monix.eval.Task
import io.grpc.ManagedChannel
import service._

import scala.util.{Failure, Success, Try}

object gclient {

  trait Implicits extends CommonRuntime {

    val channelFor: ChannelFor =
      ConfigForAddress[Try]("rpc.host", "rpc.port") match {
        case Success(c) => c
        case Failure(e) =>
          e.printStackTrace()
          throw new RuntimeException("Unable to load the client configuration", e)
    }

    implicit val serviceClient: Greeter.Client[Task] =
      Greeter.client[Task](channelFor)
  }

  object implicits extends Implicits
}
```

**Notes**:

* `host` and `port` would be read from the application configuration file.
* To be able to use the `ConfigForAddress` helper, you need to add the `frees-rpc-config` dependency to your build.

### Client Program

Once we have our runtime configuration defined as above, everything gets easier. This is an example of a client application, following our dummy quickstart:

```tut:silent
import service._
import gclient.implicits._
import monix.eval.Task

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RPCDemoApp {

  def main(args: Array[String]): Unit = {

    val result = Await.result(serviceClient.sayHello(HelloRequest("foo")).runAsync, Duration.Inf)

    println(s"Result = $result")

  }

}
```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: https://github.com/frees-io/freestyle-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalameta]: https://github.com/scalameta/scalameta
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
[frees-config]: http://frees.io/docs/patterns/config/