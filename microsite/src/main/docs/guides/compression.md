---
layout: docs
title: Compression
section: guides
permalink: /guides/compression
---

# Compression

Mu supports compression of RPC requests and responses. We can enable this
compression either on the server or the client side, or both.

Mu supports `Gzip` as the compression format.

## Server side

The server will automatically handle compressed requests from clients,
decompressing them appropriately.

To make the server compress its responses, set the `muSrcGenCompressionType` sbt
setting to `GzipGen` This will configure sbt-mu-srcgen to enable compression in
the code it generates.

## Client side

The client will automatically handle compressed responses from servers,
decompressing them appropriately.

To make the client compress its requests, you need to add the appropriate "call
option" when constructing the client.

Here is an example of a client with request compression enabled.

```scala mdoc:silent
import cats.effect.*
import higherkindness.mu.rpc.*
import io.grpc.CallOptions
import mu.examples.protobuf.greeter.*

object CompressionExampleClient {

  val channelFor: ChannelFor = ChannelForAddress("localhost", 12345)

  def clientResource[F[_]: Async]: Resource[F, Greeter[F]] =
    Greeter.client[F](channelFor, options = CallOptions.DEFAULT.withCompression("gzip"))
}
```

## Technical details

To be strictly accurate, when you enable compression on the client or server
side, the requests and responses are not compressed, but the messages inside
them are.

For example, if you enable compression on the client side, the client will
compress the message when constructing a request. It will set the `compression`
flag on the message to indicate that it is compressed, and it will set the
`grpc-encoding: gzip` request header so that the server knows how to decompress
the message.
