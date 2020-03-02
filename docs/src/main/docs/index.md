---
layout: docs
title: Mu
section: docs
permalink: /
---

# Mu-Scala

[Mu] is a toolkit designed to make it easier to build and maintain
microservices in a functional style.

While you focus on implementing the business logic for your service, let Mu take
care of the boilerplate, including:

* generation of model classes, service interfaces and clients from [Avro],
  [Protobuf] or [OpenAPI] <abbr title="Interface definition language">IDL</abbr>
  files
* serialization of requests and responses into Avro/Protobuf/JSON
* building high-performance [gRPC] servers and clients
* building HTTP REST servers and clients using [http4s]
* handling of streaming requests and responses using either [FS2] Stream or
  [Monix] Observable
* schema management and schema evolution
* metrics reporting
* ... and plenty more features on the way!

Specifically, Mu helps you to build:

* [gRPC] servers and clients based on either [Avro] or [Protobuf] protocol
  definitions
* REST servers and clients based on [OpenAPI] definitions

## Getting Started

If you're new to Mu-Scala, check out the [Getting Started guide](getting-started).

[Avro]: https://avro.apache.org/
[FS2]: https://github.com/functional-streams-for-scala/fs2
[gRPC]: https://grpc.io/
[http4s]: https://http4s.org/
[Monix]: https://monix.io/
[Mu]: https://github.com/higherkindness/mu-scala
[OpenAPI]: https://swagger.io/docs/specification/about/
[Protobuf]: https://developers.google.com/protocol-buffers
