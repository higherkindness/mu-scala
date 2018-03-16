---
layout: docs
title: IDL Generation
permalink: /docs/rpc/idl-generation
---

# IDL Generation

## Generating IDL files

Before entering implementation details, we mentioned that the [frees-rpc] ecosystem brings the ability to generate `.proto` files from the Scala definition, in order to maintain compatibility with other languages and systems outside of Scala.

This responsibility relies on `idlGen`, an sbt plugin to generate IDL files from the [frees-rpc] service definitions.

### Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.frees" % "sbt-frees-rpc-idlgen" % "0.11.1")
```

[comment]: # (End Replace)

Note that the plugin is only available for Scala 2.12, and currently only generates Protobuf `.proto` files. Avro IDL support is under consideration for development.

### Plugin Settings

There are a couple key settings that can be configured according to various needs:

* **`sourceDir`**: the Scala source directory, where your [frees-rpc] definitions are placed. By default: `baseDirectory.value / "src" / "main" / "scala"`.
* **`targetDir`**: The IDL target directory, where the `idlGen` task will write the IDL files in subdirectories such as `proto` for Protobuf, based on [frees-rpc] service definitions. By default: `baseDirectory.value / "src" / "main" / "resources"`.

Directories must exist; otherwise, the `idlGen` task will fail.

### Generation with idlGen

At this point, each time you want to update your IDL files from the scala definitions, you have to run the following sbt task:

```bash
sbt idlGen
```

Using the example above, the result would be placed at `/src/main/resources/proto/service.proto`, in the case that the scala file is named as `service.scala`. The content should be similar to:

```
// This file has been automatically generated for use by
// the idlGen plugin, from frees-rpc service definitions.
// Read more at: http://frees.io/docs/rpc/

syntax = "proto3";

option java_package = "quickstart";
option java_multiple_files = true;
option java_outer_classname = "Quickstart";

message HelloRequest {
  string greeting = 1;
}

message HelloResponse {
  string reply = 1;
}

service Greeter {
  rpc SayHello (HelloRequest) returns (HelloResponse);
  rpc LotsOfReplies (HelloRequest) returns (stream HelloResponse);
  rpc LotsOfGreetings (stream HelloRequest) returns (HelloResponse);
  rpc BidiHello (stream HelloRequest) returns (stream HelloResponse);
}
```

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