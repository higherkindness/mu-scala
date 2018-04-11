---
layout: docs
title: Core concepts
permalink: /docs/rpc/core-concepts
---

# Core concepts

## About gRPC

> [gRPC](https://grpc.io/about/) is a modern, open source, and high-performance RPC framework that can run in any environment. It can efficiently connect services in and across data centers with pluggable support for load balancing, tracing, health checking, and authentication. It's also applicable in the last mile of distributed computing to connect devices, mobile applications, and browsers to backend services.

In this project, we are focusing on the [Java gRPC] implementation.

In the upcoming sections, we'll take a look at how we can implement RPC protocols (Messages and Services) in both *gRPC* and *frees-rpc*.

## Messages and Services

### GRPC

As you might know, [gRPC] uses protocol buffers (a.k.a. *protobuf*) by default:

* As the Interface Definition Language (IDL) - for describing both the service interface and the structure of the payload messages. It is possible to use other alternatives if desired.
* For serializing/deserializing structure data - similarly, as you do with [JSON] data, defining files `.json` extension, with protobuf you have to define proto files with `.proto` as an extension.

In the example given in the [gRPC guide], you might have a proto file like this:

```
message Person {
  string name = 1;
  int32 id = 2;
  bool has_ponycopter = 3;
}
```

Then, once you’ve specified your data structures, you can use the protobuf compiler `protoc` to generate data access classes in your preferred language(s) from your proto definition.

Likewise, you can define [gRPC] services in your proto files, with RPC method parameters and return types specified as protocol buffer messages:

```
// The greeter service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply);
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

Correspondingly, [gRPC] also uses protoc with a special [gRPC] plugin to generate code from your proto file for this `Greeter` RPC service.

You can find more information about Protocol Buffers in the [Protocol Buffers' documentation](https://developers.google.com/protocol-buffers/docs/overview).

### frees-rpc

In the previous section, we’ve seen an overview of what [gRPC] offers for defining protocols and generating code (compiling protocol buffers). Now, we'll show how [frees-rpc] offers the same, but in the **Freestyle** fashion, following the FP principles.

First things first, the main difference in respect to [gRPC] is that [frees-rpc] doesn’t need `.proto` files, but it still uses protobuf, thanks to the [PBDirect] library, which allows to read and write Scala objects directly to protobuf with no `.proto` file definitions. Therefore, in summary, we have:

* Your protocols, both messages, and services, will reside with your business-logic in your Scala files using [scalameta] annotations to set them up. We’ll see more details on this shortly.
* Instead of reading `.proto` files to set up the [RPC] messages and services, [frees-rpc] offers (as an optional feature) to generate them, based on your protocols defined in your Scala code. This feature is offered to maintain compatibility with other languages and systems outside of Scala. We'll check out this feature further in [this section](/docs/rpc/idl-generation).

Let’s start looking at how to define the `Person` message that we saw previously.
Before starting, this is the Scala import we need:

```tut:silent
import freestyle.rpc.protocol._
```

`Person` definition would be defined as follows:

```tut:silent
/**
  * Message Example.
  *
  * @param name Person name.
  * @param id Person Id.
  * @param has_ponycopter Has Ponycopter check.
  */
@message
case class Person(name: String, id: Int, has_ponycopter: Boolean)
```

As we can see, it’s quite simple since it’s just a Scala case class preceded by the `@message` annotation (`@message` is optional though and used exclusively by `idlGen`):

By the same token, let’s see now how the `Greeter` service would be translated to the [frees-rpc] style (in your `.scala` file):

```tut:silent
object protocol {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  @message
  case class HelloRequest(name: String)

  /**
   * The response message,
   * @param message Message containing the greetings.
   */
  @message
  case class HelloReply(message: String)

  @service
  trait Greeter[F[_]] {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    @rpc(Protobuf) def sayHello(request: HelloRequest): F[HelloReply]

  }
}
```

Naturally, the [RPC] services are grouped in a [@tagless algebra](/docs/core/algebras/). Therefore, we are following one of the primary principles of Freestyle; you only need to concentrate on the API that you want to expose as abstract smart constructors, without worrying how they will be implemented.

In the above example, we can see that `sayHello` returns an `F[HelloReply]`. However, very often the services might:

* Return an empty response.
* Receive an empty request.
* A combination of both.

`frees-rpc` provides an `Empty` object, defined at `freestyle.rpc.protocol`, that you might want to use for these purposes.

For instance:

```tut:silent
object protocol {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  @message
  case class HelloRequest(name: String)

  /**
   * The response message,
   * @param message Message containing the greetings.
   */
  @message
  case class HelloReply(message: String)

  @service
  trait Greeter[F[_]] {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    @rpc(Protobuf) def sayHello(request: HelloRequest): F[HelloReply]

    @rpc(Protobuf) def emptyResponse(request: HelloRequest): F[Empty.type]

    @rpc(Protobuf) def emptyRequest(request: Empty.type): F[HelloReply]

    @rpc(Protobuf) def emptyRequestRespose(request: Empty.type): F[Empty.type]
  }
}
```

We are also using some additional annotations:

* `@service`: tags the trait as an [RPC] service, in order to derive server and client code (macro expansion).
* `@rpc(Protobuf)`: indicates that the method is an RPC service. It receives as argument the type of serialization that will be used to encode/decode data, `Protocol Buffers` in the example. `Avro` is also supported as another type of serialization.

We'll see more details about these and other annotations in the following sections.

## Compression

[frees-rpc] allows us to compress the data we are sending in our services. We can enable this compression either on the server or the client side.

[frees-rpc] supports `Gzip` as compression format.

On the server side, we only have to add the annotation `Gzip` in our defined services.

Let's see an example of a unary service on the server side.

```tut:silent
object service {

  @service
  trait Greeter[F[_]] {

    /**
     * Unary RPC with Gzip compression
     *
     * @param empty Client request.
     * @return empty server response.
     */
    @rpc(Protobuf, Gzip) def emptyCompressed(empty: Empty.type): F[Empty.type] 

  }

}
```

On the client, to enable it, we only have to add an option to the client in the channel builder.

Let's see an example of a client with the compression enabled.

```tut:invisible
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

}
```

```tut:silent
import cats.implicits._
import freestyle.free.config.implicits._
import freestyle.async.catsEffect.implicits._
import freestyle.rpc._
import freestyle.rpc.client._
import freestyle.rpc.client.config._
import freestyle.rpc.client.implicits._
import monix.eval.Task
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import service._
import scala.util.{Failure, Success, Try}

trait Implicits extends CommonRuntime {

  val channelFor: ChannelFor =
    ConfigForAddress[Try]("rpc.host", "rpc.port") match {
      case Success(c) => c
      case Failure(e) =>
        e.printStackTrace()
        throw new RuntimeException("Unable to load the client configuration", e)
    }

  implicit val serviceClient: Greeter.Client[Task] =
    Greeter.client[Task](channelFor, options = CallOptions.DEFAULT.withCompression("gzip"))
}

object implicits extends Implicits

```

## Service Methods

As [gRPC], [frees-rpc] allows you to define two main kinds of service methods:

* **Unary RPC**: the simplest way of communication, one client request, and one server response.
* **Streaming RPC**: similar to the unary, but depending the kind of streaming, the client or server or both will send back a stream of responses. There are three kinds of streaming, server, client and bidirectional streaming.

Let's complete our protocol's example with an unary service method:

```tut:silent
object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service
  trait Greeter[F[_]] {

    /**
     * Unary RPC where the client sends a single request to the server and gets a single response back,
     * just like a normal function call.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client request.
     * @return Server response.
     */
    @rpc(Protobuf)
    def sayHello(request: HelloRequest): F[HelloResponse]

  }

}
```

Here `sayHello` is our unary RPC, where only the `@rpc` annotation is needed.

In the `Streaming` section, we are going to see all the streaming options.

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
[]: https://github.com/frees-io/freestyle-rpc/modules/examples
[Metrifier]: https://github.com/47deg/metrifier
[frees-config]: http://frees.io/docs/patterns/config/
