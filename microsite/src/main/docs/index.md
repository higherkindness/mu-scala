---
layout: docs
title: Mu
section: docs
permalink: /
---

# Mu-Scala

[Mu] is a suite of libraries and tools that help you build and maintain
microservices and clients in a functional style.

## Getting Started

If you're new to Mu-Scala, check out the [Getting Started
guide](getting-started) and the [tutorials](tutorials).

## Features

While you focus on implementing the business logic for your service, let Mu take
care of the boilerplate and non-functional requirements, including:

* generation of model classes, service interfaces and clients from [Avro],
  or [Protobuf] <abbr title="Interface definition language">IDL</abbr>
  files
* serialization of requests and responses into Avro/Protobuf
* building high-performance [gRPC] servers and clients
* handling of [streaming requests and responses](guides/grpc-streaming) using [FS2] Stream
* [accessing metadata on services](guides/accessing-metadata)
* [distributed tracing](guides/distributed-tracing)
* [metrics reporting](guides/metrics-reporting)
* ... and plenty more features on the way!

Specifically, Mu helps you to build [gRPC] servers and clients based on either
[Avro] or [Protobuf] protocol definitions.

[Avro]: https://avro.apache.org/
[FS2]: https://github.com/typelevel/fs2
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu-scala
[Protobuf]: https://developers.google.com/protocol-buffers
