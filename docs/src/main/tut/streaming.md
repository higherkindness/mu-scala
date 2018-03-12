---
layout: docs
title: Streaming
permalink: /docs/rpc/streaming
---

# Streaming

In the previous section, we saw that [frees-rpc] allows you to define unary or streaming service methods.

The streaming service methods are the next:

* **Server streaming RPC**: similar to the unary, but in this case, the server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client who sends a stream of requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: it would be a mix of server and client streaming since both sides will be sending a stream of data.

Let's keep going completing our protocol's example with the three streaming service methods:

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

* `lotsOfReplies `: Server streaming RPC, where `@rpc` and `@stream` annotations are needed here. However, there are three different types of streaming (server, client and bidirectional), that are specified by the type parameter required in the `@stream` annotation, `@stream[ResponseStreaming.type]` in this particular definition.
* `lotsOfGreetings `: Client streaming RPC, `@rpc` should be sorted by the `@stream[RequestStreaming.type]` annotation.
* `bidiHello `: Bidirectional streaming RPC, where `@rpc` is accompanied by the `@stream[BidirectionalStreaming.type]` annotation.


[frees-rpc] provides the ability to combine [RPC] protocols, services, and clients in your `Freestyle` program, thanks to [gRPC]. Although it's fully integrated with [gRPC], there are some important differences when defining the protocols, as we’ll see later on, since [frees-rpc] follows the same philosophy as `Freestyle` core, being macro-powered.

## Integrations:

In [frees-rpc], the streaming features have been implemented based in two libraries. 

### Observable

The first library is `monix.reactive.Observable`, see the [Monix Docs](https://monix.io/docs/2x/reactive/observable.html) for a wider explanation. These monix extensions have been implemented on top of the [gRPC Java API](https://grpc.io/grpc-java/javadoc/) and the `StreamObserver` interface.

### FS2: Functional Streams

The second library is `fs2` streaming, see the [FS2 Docs](https://github.com/functional-streams-for-scala/fs2) It's considered experimental for now but it works fine.