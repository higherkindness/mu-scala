---
layout: docs
title: IDL Generation
permalink: /docs/rpc/idl-generation
---

# IDL Generation

Before entering implementation details, we mentioned that the [frees-rpc] ecosystem brings the ability to generate `.proto` files from the Scala definition, in order to maintain compatibility with other languages and systems outside of Scala.

This relies on `idlGen`, an sbt plugin to generate Protobuf and Avro IDL files from the [frees-rpc] service definitions.

## Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.frees" % "sbt-frees-rpc-idlgen" % "0.13.2")
```

[comment]: # (End Replace)

Note that the plugin is only available for Scala 2.12.

## Generating IDL files from source

Let's take our previous service, and add an Avro-specific request and some useful annotations described in the [Annotations section](/docs/rpc/annotations):
```tut:silent
import freestyle.rpc.protocol._

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

At this point, each time you want to update your `.proto` IDL files from the scala definitions, you have to run the following sbt task:
```bash
sbt "idlGen proto"
```
Using this example, the resulting Protobuf IDL would be generated in `/src/main/resources/proto/service.proto`, in the case that the scala file is named `service.scala`. The content should be similar to:

```
// This file has been automatically generated for use by
// the idlGen plugin, from frees-rpc service definitions.
// Read more at: http://frees.io/docs/rpc/

syntax = "proto3";

option java_multiple_files = true;
option java_outer_class_name = "Quickstart";

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

To generate Avro IDL instead, use:
```bash
sbt "idlGen avro"
```
And the resulting Avro IDL would be generated in `target/scala-2.12/resource_managed/main/avro/service.avpr`:

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

### Plugin Settings

When using `idlGen`, there are a couple key settings that can be configured according to various needs:

* **`idlType`**: the type of IDL to be generated, either `proto` or `avro`.
* **`idlGenSourceDir`**: the Scala source base directory, where your [frees-rpc] definitions are placed. By default: `Compile / sourceDirectory`, typically `src/main/scala/`.
* **`idlGenTargetDir`**: the IDL target base directory, where the `idlGen` task will write the IDL files in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on [frees-rpc] service definitions. By default: `Compile / resourceManaged`, typically `target/scala-2.12/resource_managed/main/`.

The source directory must exist, otherwise, the `idlGen` task will fail. Target directories will be created upon generation.

## Generating source files from IDL

You can also use this process in reverse and generate [frees-rpc] Scala classes from IDL definitions. Currently only Avro is supported, in both `.avpr` (JSON) and `.avdl` (Avro IDL) formats.
The plugin's implementation basically wraps the [avrohugger] library and adds some freestyle-specific extensions.

To use it, run:
```bash
sbt "srcGen avro"
```

You could even use `IDL` definitions packaged into artifacts, within your classpath. In that particular situation, you need to setup `srcJarNames`, specifying the artifacts names that will be unzipped to extract the `IDL` files. You need to run:
```bash
sbt "srcGenFromJars"
```

`srcGenFromJars` can very useful when you want to distribute your `IDL` files without binary code (to prevent binary conflicts in clients). In that case, you might want to include some additional settings in the build where your `IDL` files are placed, like for instance:
```
//...
.settings(
  Seq(
    publishMavenStyle := true,
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    idlType := "avro",
    srcGenSourceDir := (Compile / resourceDirectory).value,
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (srcGen in Compile).taskValue
  )
)
//...
```

In the case of the `.avpr` file we generated above, the file `GreeterService.scala` would be generated in `target/scala-2.12/src_managed/main/quickstart`:
```
package quickstart

import freestyle.rpc.protocol._

@message case class HelloRequest(greeting: String)

@message case class HelloResponse(reply: String)

@service trait GreeterService[F[_]] {

  @rpc(Avro)
  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]

}
```

You can also integrate this source generation in your compile process by adding this setting to your module:
```
sourceGenerators in Compile += (srcGen in Compile).taskValue)
```
or,
```
sourceGenerators in Compile += (srcGenFromJars in Compile).taskValue)
```

### Plugin Settings

Just like `idlGen`, `srcGen` and `srcGenFromJars` has some configurable settings:

* **`idlType`**: the type of IDL to generate from, currently only `avro`.
* **`srcGenSerializationType`**: the serialization type when generating Scala sources from the IDL definitions. `Protobuf`, `Avro` or `AvroWithSchema` are the current supported serialization types. By default, the serialization type is `Avro`.
* **`srcJarNames`**: the list of jar names containing the IDL definitions that will be used at compilation time by `srcGenFromJars` to generate the Scala Sources. By default, this sequence is empty.
* **`srcGenSourceDir`**: the IDL source base directory, where your IDL files are placed. By default: `Compile / resourceDirectory`, typically `src/main/resources/`.
* **`srcGenSourceDirs`**: the list of directories where your IDL files are placed. By default, it contains `srcGenSourceDir.value` and `srcGenSourceFromJarsDir.value` folders.
* **`srcGenIDLTargetDir`**: the directory where all the IDL files will be placed. By default, it's defined as `(Compile / resourceManaged).value / idlType.value`, typically `target/scala-2.12/src_managed/main/avro`. Given this configuration, the plugin will automatically copy to this target directory:
  * All the definitions extracted from the different `jar` files, and also,
  * All the source folders specified in the `srcGenSourceDirs` setting.
* **`srcGenTargetDir`**: the Scala target base directory, where the `srcGen` task will write the Scala files in subdirectories/packages based on the namespaces of the IDL files. By default: `Compile / sourceManaged`, typically `target/scala-2.12/src_managed/main/`.
* **`genOptions`**: additional options to add to the generated `@rpc` annotations, after the IDL type. Currently only supports `"Gzip"`.

The source directory must exist, otherwise, the `srcGen` task will fail. Target directories will be created upon generation.

*Note*: regarding `srcGenSourceDirs`, all the directories configured as the source, will be distributed in the resulting jar artifact preserving the same folder structure as in the source.

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
[avrohugger]: https://github.com/julianpeeters/avrohugger