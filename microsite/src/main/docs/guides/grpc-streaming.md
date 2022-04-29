---
layout: docs
title: gRPC Streaming
section: guides
permalink: /guides/grpc-streaming
---

# gRPC Streaming

In the [tutorials](../tutorials), we only defined gRPC services with so-called
"unary" endpoints. This is an endpoint that does not involve streaming. The
client sends a single request and receives a single response.

[gRPC] also defines the following kinds of streaming, all of which are supported
by Mu.

* **Server streaming RPC**: similar to the unary service, but in this case the
  server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client which sends a stream of
  requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: a mix of server and client streaming as both
  sides will be sending a stream of data.

## Protobuf

Mu only officially supports streaming for Protobuf, not Avro. This is because
Avro does not (yet) have support for streaming RPC endpoints in its protocol
specification.

The relevant Avro issue is
[AVRO-406](https://issues.apache.org/jira/browse/AVRO-406).

## Stream implementation

Mu uses [FS2 Stream](https://github.com/typelevel/fs2) for streaming of RPC
requests and responses.

## Service definition with streaming endpoints

Let's see what a Mu RPC service definition looks like when we introduce
streaming endpoints.

Here is an example `.proto` file:

```proto
syntax = "proto3";

package mu.examples.protobuf.greeter;

message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string greeting = 1;
}

service StreamingGreeter {
  rpc LotsOfHellos (stream HelloRequest) returns (HelloResponse);

  rpc LotsOfReplies (HelloRequest) returns (stream HelloResponse);

  rpc BidirectionalHello (stream HelloRequest) returns (stream HelloResponse);
}
```

The service defines 3 RPC endpoints:

* `LotsOfHellos` is client-streaming
* `LotsOfReplies` is server-streaming
* `BidirectionalHello` is bidirectional streaming

## Service definition in Scala

If we use the sbt-mu-srcgen plugin to generate Scala code, it will output a
service definition that looks like this (cleaned up for readability):

```scala
package mu.examples.protobuf.greeter.streaming

trait StreamingGreeter[F[_]] {

  def LotsOfHellos(req: Stream[F, HelloRequest]): F[HelloResponse]

  def LotsOfReplies(req: HelloRequest): F[Stream[F, HelloResponse]]

  def BidirectionalHello(req: Stream[F, HelloRequest]): F[Stream[F, HelloResponse]]

}

object StreamingGreeter {

  // ... lots of generated code

}
```

## Service implementation example

An implementation of this service on the server side might look something like
this:

```scala mdoc:silent
import mu.examples.protobuf.greeter.streaming._
import cats.effect.Concurrent
import cats.syntax.all._
import fs2.Stream

class MyStreamingGreeter[F[_]: Concurrent] extends StreamingGreeter[F] {

  def LotsOfHellos(reqStream: Stream[F, HelloRequest]): F[HelloResponse] =
    reqStream.compile.toList.map { requests =>
      val names = requests.map(_.name).mkString(" and ")
      HelloResponse(s"Hello, $names")
    }

  def LotsOfReplies(req: HelloRequest): F[Stream[F, HelloResponse]] =
    Stream(
      HelloResponse(s"Hello, ${req.name}"),
      HelloResponse(s"Hello again, ${req.name}")
    ).covary[F].pure[F]

  def BidirectionalHello(reqStream: Stream[F, HelloRequest]): F[Stream[F, HelloResponse]] =
    reqStream.map(req => HelloResponse(s"Hello, ${req.name}")).pure[F]

}
```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
