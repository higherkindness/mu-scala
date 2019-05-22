---
layout: docs
title: Streaming
permalink: /streaming
---

# Streaming

In the previous section, we saw that [mu] allows you to define unary services. Additionally, it supports the following streaming options:

* **Server streaming RPC**: similar to the unary service, but in this case the server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client which sends a stream of requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: a mix of server and client streaming as both sides will be sending a stream of data.

Let's keep going. We'll be completing our protocol's example with the three streaming options:

```tut:silent
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
     * Server streaming RPC where the client sends a request to the server and gets a stream to read a
     * sequence of messages back. The client reads from the returned stream until there are no more messages.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Single client request.
     * @return Stream of server responses.
     */
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
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]

  }

}
```

This code might be self-explanatory but let's review the different services one by one:

* `lotsOfReplies `: Server streaming RPC.
* `lotsOfGreetings `: Client streaming RPC.
* `bidiHello `: Bidirectional streaming RPC.

## Integrations

In [mu], the streaming features have been implemented based on two data types. You can choose whichever data type fits your application's needs best.

### Observable

The first data type is `monix.reactive.Observable`, see the [Monix Docs](https://monix.io/docs/2x/reactive/observable.html) for a more detailed explanation. These monix extensions have been implemented on top of the [gRPC Java API](https://grpc.io/grpc-java/javadoc/) and the `StreamObserver` interface.

The example above shows a basic implementation of how to use this data type for streaming.

### FS2: Functional Streams

The second data type available for implementing streaming protocols is `fs2.Stream`, see the [FS2 Docs](https://github.com/functional-streams-for-scala/fs2) for more details. 

Thanks to this new data type, [mu] supports `fs2.Stream[F, ?]` for all the types of streaming mentioned before.

Let's compare our previous protocols using `fs2.Stream` instead of `Observable`.

```tut:silent
import higherkindness.mu.rpc.protocol._

object service {

  import fs2.Stream

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * Server streaming RPC 
     *
     * @param request Single client request.
     * @return Stream of server responses.
     */
    def lotsOfReplies(request: HelloRequest): Stream[F, HelloResponse]

    /**
     * Client streaming RPC 
     *
     * @param request Stream of client requests.
     * @return Single server response.
     */
    def lotsOfGreetings(request: Stream[F, HelloRequest]): F[HelloResponse]

    /**
     * Bidirectional streaming RPC 
     *
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    def bidiHello(request: Stream[F, HelloRequest]): Stream[F, HelloResponse]

  }

}
```

As you can see, the Fs2 service is very similar to the Observable service.

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

