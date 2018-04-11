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
@rpc | `Method` | (`SerializationType`, `Compression`) | Indicates that the method is an RPC request. As `SerializationType` parameter value, `Protobuf` and `Avro` are the current supported serialization methods. As `Compression` parameter value, only `Gzip` is supported.
@stream | `Method` | `[S <: StreamingType]` | Indicates the  method's streaming type: server, client, and bidirectional. Hence, the `S` type parameter can be `ResponseStreaming`, `RequestStreaming`, or `BidirectionalStreaming`, respectively.
@message | `Case Class` | - | Tags the case class as an RPC message.
@option | `Object` | `(name: String, value: Any)` | Defines the equivalent headers in `.proto` files.
@outputPackage | `Object` | `(value: String)` | Defines the `package` declaration in `.proto` files, and the `namespace` tag in `.avpr` files.
@outputName  | `Object` | `(value: String)` | Defines the `protocol` tag in `.avpr` files; if missing the `idlGen` tool will use the source file name (without `.scala` extension) instead.

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