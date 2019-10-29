---
layout: docs
title: IDL Generation
permalink: /idl-generation
---


# IDL Generation

As we said before, this feature is deprecated.  Even so, let's see how it works.

The ability to generate `.proto`files from Scala definitions relies on `idlGen`, an sbt plugin to generate `Protobuf` and `Avro` IDL files from the [Mu] service definitions.

## Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-idlgen" % "0.19.1")
```

[comment]: # (End Replace)

Note that the plugin is only available for Scala 2.12.

## Generating IDL files from source

Let's take our previous service and add an Avro-specific request and some useful annotations described in the [Annotations section](annotations):

```tut:silent
import higherkindness.mu.rpc.protocol._

@option("java_multiple_files", true)
@option("java_outer_classname", "Quickstart")
@outputName("GreeterService")
@outputPackage("quickstart")
object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service(Protobuf)
  trait ProtoGreeter[F[_]] {

    def sayHello(request: HelloRequest): F[HelloResponse]

    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]
    
  }
  
  @service(Avro)
  trait AvroGreeter[F[_]] {
    def sayHelloAvro(request: HelloRequest): F[HelloResponse]
  }
}
```
Note that, ``HelloResponse`` and `HelloRequest` case classes are preceded by the `@message` annotation.

At this point, each time you want to update your `.proto` IDL files from the scala definitions, 
you have to run the following sbt task:

```bash
sbt "idlGen proto"
```

However, as we said in [the previous section](generate-sources-from-idl), the easiest way to use the plugin is integrating the source generation in your compile process by adding this import to your `build.sbt` file:

```scala
 import higherkindness.mu.rpc.idlgen.IdlGenPlugin.autoImport._
``` 
and the setting,   
                                                                                
```scala  
 idlType := proto  
 sourceGenerators in Compile += (idlGen in Compile).taskValue
```

Using this example, the resulting `Protobuf` IDL would be generated in `/src/main/resources/proto/service.proto`, 
assuming that the scala file is named `service.scala`. The content should be similar to:

```proto
// This file has been automatically generated for use by
// the idlGen plugin, from mu-rpc service definitions.
// Read more at: https://higherkindness.github.io/mu/scala/

syntax = "proto3";

option java_multiple_files = true;

package quickstart;

message HelloRequest {
  string greeting = 1;
}

message HelloResponse {
  string reply = 1;
}

service ProtoGreeter {
  rpc SayHello (HelloRequest) returns (HelloResponse);
  rpc LotsOfReplies (HelloRequest) returns (stream HelloResponse);
  rpc LotsOfGreetings (stream HelloRequest) returns (HelloResponse);
  rpc BidiHello (stream HelloRequest) returns (stream HelloResponse);
}
```

To generate `Avro` IDL instead, use:

```bash
sbt "idlGen avro"
```
or change the settings in your `build.sbt`:

```scala  
 idlType := avro  
 sourceGenerators in Compile += (idlGen in Compile).taskValue
```

And the resulting `Avro` IDL would be generated in `target/scala-2.12/resource_managed/main/avro/service.avpr`:

```json
{
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

Note that due to limitations in the `Avro` IDL, currently only unary RPC services 
are converted (client and/or server-streaming services are ignored).

### Plugin Settings

When using `idlGen`, there are a couple key settings that can be configured according to various needs:

* **`idlType`**: the type of IDL to be generated, either `proto` or `avro`.
* **`idlGenSourceDir`**: the Scala source base directory, where your [Mu] definitions are placed. By default: `Compile / sourceDirectory`, typically `src/main/scala/`.
* **`idlGenTargetDir`**: the IDL target base directory, where the `idlGen` task will write the IDL files in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on [Mu] service definitions. By default: `Compile / resourceManaged`, typically `target/scala-2.12/resource_managed/main/`.

The source directory must exist, otherwise, the `idlGen` task will fail. Target directories will be created upon generation.


[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/47deg/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
[avrohugger]: https://github.com/julianpeeters/avrohugger
[skeuomorph]: https://github.com/higherkindness/skeuomorph