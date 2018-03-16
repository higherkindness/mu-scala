---
layout: docs
title: Annotations
permalink: /docs/rpc/annotations
---

# Annotations

Provided below is a summary of all the current annotations that [frees-rpc] provides:

Annotation | Scope | Arguments | Description
--- | --- | --- | ---
@service | `Trait` | - | Tags the trait as an [RPC] service, in order to derive server and client code (macro expansion).
@rpc | `Method` | (`SerializationType`, `Compression`) | Indicates the method is an RPC request. As `SerializationType` parameter value, `Protobuf` and `Avro` are the current supported serialization methods. As `Compression` parameter value is `Gzip`.
@stream | `Method` | [`S <: StreamingType`] | Indicates the  method's streaming type: server, client, and bidirectional. Hence, the `S` type parameter can be `ResponseStreaming`, `RequestStreaming`, or `BidirectionalStreaming`, respectively.
@message | `Case Class` | - | Tags a case class an RPC message.
@option | `Object` | [name: String, value: String, quote: Boolean] | Used to define the equivalent headers in `.proto` files

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
[freestyle-rpc-examples]: https://github.com/frees-io/freestyle-rpc-examples
[Metrifier]: https://github.com/47deg/metrifier
[frees-config]: http://frees.io/docs/patterns/config/