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
@rpc | `Method` | (`SerializationType`) | Indicates the method is an RPC request. As `SerializationType` parameter value, `Protobuf` and `Avro` are the current supported serialization methods.
@stream | `Method` | [`S <: StreamingType`] | Indicates the  method's streaming type: server, client, and bidirectional. Hence, the `S` type parameter can be `ResponseStreaming`, `RequestStreaming`, or `BidirectionalStreaming`, respectively.
@message | `Case Class` | - | Tags a case class an RPC message.
@option | `Object` | [name: String, value: String, quote: Boolean] | Used to define the equivalent headers in `.proto` files