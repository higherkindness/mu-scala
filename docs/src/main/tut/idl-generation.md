---
layout: docs
title: IDL Generation
permalink: /docs/rpc/idl-generation
---

# IDL Generation

## Generating IDL files

Before entering implementation details, we mentioned that the [frees-rpc] ecosystem brings the ability to generate `.proto` files from the Scala definition, in order to maintain compatibility with other languages and systems outside of Scala.

This responsibility relies on `idlGen`, an sbt plugin to generate Protobuf and Avro IDL files from the [frees-rpc] service definitions.

### Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.frees" % "sbt-frees-rpc-idlgen" % "0.12.0")
```

[comment]: # (End Replace)

Note that the plugin is only available for Scala 2.12.

### Plugin Settings

There are a couple key settings that can be configured according to various needs:

* **`sourceDir`**: the Scala source directory, where your [frees-rpc] definitions are placed. By default: `baseDirectory.value / "src" / "main" / "scala"`.
* **`targetDir`**: The IDL target directory, where the `idlGen` task will write the IDL files in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on [frees-rpc] service definitions. By default: `baseDirectory.value / "src" / "main" / "resources"`.

Base directories must exist, otherwise, the `idlGen` task will fail. Subdirectories will be created upon generation.

### Generation with idlGen

At this point, each time you want to update your IDL files from the scala definitions, you have to run the following sbt task:

```bash
sbt idlGen
```

Let's take our previous service, and add an Avro-specific request and some useful annotations described in the [Annotations section](/docs/rpc/annotations):
```tut:silent
import freestyle.rpc.protocol._

@option(name = "java_multiple_files", value = true)
@option(name = "java_outer_classname", value = "Quickstart")
@outputName("GreeterService")
@outputPackage("quickstart")
object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service
  trait Greeter[F[_]] {

    @rpc(Protobuf)
    def sayHello(request: HelloRequest): F[HelloResponse]
     
    @rpc(Avro)
    def sayHelloAvro(request: HelloRequest): F[HelloResponse]

    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    @rpc(Protobuf)
    @stream[RequestStreaming.type]
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]
    
  }
  
}
```

Using this example, the resulting Protobuf IDL would be generated in `/src/main/resources/proto/service.proto`, in the case that the scala file is named `service.scala`. The content should be similar to:

```
// This file has been automatically generated for use by
// the idlGen plugin, from frees-rpc service definitions.
// Read more at: http://frees.io/docs/rpc/

syntax = "proto3";

option java_package = "quickstart";
option java_multiple_files = true;

package quickstart;

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

And the resulting Avro IDL would be generated in `/src/main/resources/avro/service.avpr`:

```
{
  "namespace" : "quickstart",
  "protocol" : "GreeterService",
  "types" : [
    {
      "name" : "HelloRequest",
      "type" : "record",
      "fields" : [
        {
          "name" : "greeting",
          "type" : "string"
        }
      ]
    },
    {
      "name" : "HelloResponse",
      "type" : "record",
      "fields" : [
        {
          "name" : "reply",
          "type" : "string"
        }
      ]
    }
  ],
  "messages" : {
    "sayHelloAvro" : {
      "request" : [
        {
          "name" : "arg",
          "type" : "HelloRequest"
        }
      ],
      "response" : "HelloResponse"
    }
  }
}
```

Note that due to limitations in the Avro IDL, currently only unary RPC services are converted (client- and/or server-streaming services are ignored).

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