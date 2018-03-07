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
* Instead of reading `.proto` files to set up the [RPC] messages and services, [frees-rpc] offers (as an optional feature) to generate them, based on your protocols defined in your Scala code. This feature is offered to maintain compatibility with other languages and systems outside of Scala. We'll check out this feature further in [this section](#generating-idl-files).

Let’s start looking at how to define the `Person` message that we saw previously.
Before starting, these are the Scala imports we need:

```tut:silent
import freestyle.free._
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
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object protocols {

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

Naturally, the [RPC] services are grouped in a [@tagless algebra]. Therefore, we are following one of the primary principles of Freestyle; you only need to concentrate on the API that you want to expose as abstract smart constructors, without worrying how they will be implemented.

In the above example, we can see that `sayHello` returns a `FS[HelloReply]`. However, very often the services might:

* Return an empty response.
* Receive an empty request.
* A combination of both.

`frees-rpc` provides an `Empty` object, defined at `freestyle.rpc.protocol`, that you might want to use for these purposes.

For instance:

```tut:silent
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
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

* `@option`: used to define the equivalent headers in `.proto` files.
* `@service`: tags the trait as an [RPC] service, in order to derive server and client code (macro expansion).
* `@rpc(Protobuf)`: indicates that the method is an RPC service. It receives as argument the type of serialization that will be used to encode/decode data, `Protocol Buffers` in the example. `Avro` is also supported as another type of serialization.

We'll see more details about these and other annotations in the following sections.

## Service Methods

As [gRPC], [frees-rpc] allows you to define four kinds of service methods:

* **Unary RPC**: the simplest way of communication, one client request, and one server response.
* **Server streaming RPC**: similar to the unary, but in this case, the server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client who sends a stream of requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: it would be a mix of server and client streaming since both sides will be sending a stream of data.

Let's complete our protocol's example with these four kinds of service methods:

```tut:silent
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
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

    /**
     * Server streaming RPC where the client sends a request to the server and gets a stream to read a
     * sequence of messages back. The client reads from the returned stream until there are no more messages.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Single client request.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    /**
     * Client streaming RPC where the client writes a sequence of messages and sends them to the server,
     * again using a provided stream. Once the client has finished writing the messages, it waits for
     * the server to read them and return its response.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of client requests.
     * @return Single server response.
     */
    @rpc(Protobuf)
    @stream[RequestStreaming.type]
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    /**
     * Bidirectional streaming RPC where both sides send a sequence of messages using a read-write stream.
     * The two streams operate independently, so clients and servers can read and write in whatever order
     * they like: for example, the server could wait to receive all the client messages before writing its
     * responses, or it could alternately read a message then write a message, or some other combination of
     * reads and writes. The order of messages in each stream is preserved.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]

  }

}
```

The code might be explanatory by itself but let's review the different services one by one:

* `sayHello`: unary RPC, only the `@rpc` annotation would be needed in this case.
* `lotsOfReplies `: Server streaming RPC, where `@rpc` and `@stream` annotations are needed here. However, there are three different types of streaming (server, client and bidirectional), that are specified by the type parameter required in the `@stream` annotation, `@stream[ResponseStreaming.type]` in this particular definition.
* `lotsOfGreetings `: Client streaming RPC, `@rpc` should be sorted by the `@stream[RequestStreaming.type]` annotation.
* `bidiHello `: Bidirectional streaming RPC, where `@rpc` is accompanied by the `@stream[BidirectionalStreaming.type]` annotation.

**Notes**:

* In [frees-rpc], the streaming features have been implemented with `monix.reactive.Observable`, see the [Monix Docs](https://monix.io/docs/2x/reactive/observable.html) for a wider explanation. These monix extensions have been implemented on top of the [gRPC Java API](https://grpc.io/grpc-java/javadoc/) and the `StreamObserver` interface.
* After [this PR](https://github.com/frees-io/freestyle-rpc/pull/152), `fs2` streaming is also supported but it's considered experimental for now.