---
layout: docs
title: IDL Generation
permalink: /idl-generation
---

# IDL Generation

Before going into implementation details, we mentioned that the [mu] ecosystem gives us the ability to generate `.proto` files from Scala definitions, in order to maintain compatibility with other languages and systems outside of Scala.

This relies on `idlGen`, an sbt plugin to generate Protobuf and Avro IDL files from the [mu] service definitions.

## Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-idlgen" % "0.18.0")
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

At this point, each time you want to update your `.proto` IDL files from the scala definitions, you have to run the following sbt task:

```bash
sbt "idlGen proto"
```

Using this example, the resulting Protobuf IDL would be generated in `/src/main/resources/proto/service.proto`, assuming that the scala file is named `service.scala`. The content should be similar to:

```
// This file has been automatically generated for use by
// the idlGen plugin, from mu-rpc service definitions.
// Read more at: https://higherkindness.github.io/mu/scala/

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

service ProtoGreeter {
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

Note that due to limitations in the Avro IDL, currently only unary RPC services are converted (client and/or server-streaming services are ignored).

### Plugin Settings

When using `idlGen`, there are a couple key settings that can be configured according to various needs:

* **`idlType`**: the type of IDL to be generated, either `proto` or `avro`.
* **`idlGenSourceDir`**: the Scala source base directory, where your [mu] definitions are placed. By default: `Compile / sourceDirectory`, typically `src/main/scala/`.
* **`idlGenTargetDir`**: the IDL target base directory, where the `idlGen` task will write the IDL files in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on [mu] service definitions. By default: `Compile / resourceManaged`, typically `target/scala-2.12/resource_managed/main/`.

The source directory must exist, otherwise, the `idlGen` task will fail. Target directories will be created upon generation.

## Generating source files from IDL

You can also use this process in reverse and generate [mu] Scala classes from IDL definitions. Currently only Avro is supported, in both `.avpr` (JSON) and `.avdl` (Avro IDL) formats.
The plugin's implementation basically wraps the [avrohugger] library and adds some mu-specific extensions.

To use it, run:

```bash
sbt "srcGen avro"
```

You can even use `IDL` definitions packaged into artifacts within your classpath. In that particular situation, you need to setup `srcGenJarNames`, specifying the artifact names (or sbt module names) that will be unzipped/used to extract the `IDL` files.

`srcGenJarNames ` can be very useful when you want to distribute your `IDL` files without binary code (to prevent binary conflicts in clients). In that case, you might want to include some additional settings in the build where your `IDL` files are placed, for instance:

```
//...
.settings(
  Seq(
    publishMavenStyle := true,
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    idlType := "avro",
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / srcGen).taskValue
  )
)
//...
```

In the case of the `.avpr` file we generated above, the file `GreeterService.scala` would be generated in `target/scala-2.12/src_managed/main/quickstart`:

```
package quickstart

import higherkindness.mu.rpc.protocol._

@message case class HelloRequest(greeting: String)

@message case class HelloResponse(reply: String)

@service trait GreeterService[F[_]] {

  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]

}
```

You can also integrate this source generation in your compile process by adding this setting to your build:

```
sourceGenerators in Compile += (Compile / srcGen).taskValue
```

### Plugin Settings

Just like `idlGen`, `srcGen` has some configurable settings:

* **`idlType`**: the type of IDL to generate from, currently only `avro`.
* **`srcGenSerializationType`**: the serialization type when generating Scala sources from the IDL definitions. `Protobuf`, `Avro` or `AvroWithSchema` are the current supported serialization types. By default, the serialization type is `Avro`.
* **`srcGenJarNames`**: the list of jar names or sbt modules containing the IDL definitions that will be used at compilation time by `srcGen` to generate the Scala sources. By default, this sequence is empty.
* **`srcGenSourceDirs`**: the list of directories where your IDL files are placed. By default: `Compile / resourceDirectory`, typically `src/main/resources/`.
* **`srcGenIDLTargetDir`**: the directory where all the IDL files will be placed. By default, it's defined as `(Compile / resourceManaged).value / idlType.value`, typically `target/scala-2.12/resource_managed/main/avro`. Given this configuration, the plugin will automatically copy the following to this target directory:
  * All the definitions extracted from the different `jar` or `sbt` modules, and also,
  * All the source folders specified in the `srcGenSourceDirs` setting.
* **`srcGenTargetDir`**: the Scala target base directory, where the `srcGen` task will write the Scala files in subdirectories/packages based on the namespaces of the IDL files. By default: `Compile / sourceManaged`, typically `target/scala-2.12/src_managed/main/`.
* **`genOptions`**: additional options to add to the generated `@service` annotations, after the IDL type. Currently only supports `"Gzip"`.
* **`idlGenBigDecimal`**: specifies how the `decimal` types will be generated. `ScalaBigDecimalGen` produces `scala.math.BigDecimal` and `ScalaBigDecimalTaggedGen` produces `scala.math.BigDecimal` but tagged with the 'precision' and 'scale'. i.e. `scala.math.BigDecimal @@ (Nat._8, Nat._2)`. By default `ScalaBigDecimalTaggedGen`.
* **`idlGenMarshallerImports`**: additional imports to add on top to the generated service files. This property can be used for importing extra codecs for your services. By default:
  * `List(BigDecimalAvroMarshallers, JavaTimeDateAvroMarshallers)` if `srcGenSerializationType` is `Avro` or `AvroWithSchema` and `idlGenBigDecimal` is `ScalaBigDecimalGen`
  * `List(BigDecimalTaggedAvroMarshallers, JavaTimeDateAvroMarshallers)` if `srcGenSerializationType` is `Avro` or `AvroWithSchema` and `idlGenBigDecimal` is `ScalaBigDecimalTaggedGen`
  * `List(BigDecimalProtobufMarshallers, JavaTimeDateProtobufMarshallers)` if `srcGenSerializationType` is `Protobuf`.

The `JodaDateTimeAvroMarshallers` and `JodaDateTimeProtobufMarshallers` are also available, but they need the dependency `mu-rpc-marshallers-jodatime`. You can also specify custom imports with the following:
  * `idlGenMarshallerImports := List(higherkindness.mu.rpc.idlgen.Model.CustomMarshallersImport("com.sample.marshallers._"))`
  * See the [Custom codecs section in core concepts](core-concepts#custom-codecs) for more information.

The source directory must exist, otherwise, the `srcGen` task will fail. Target directories will be created upon generation.

*Note*: regarding `srcGenSourceDirs`, all the directories configured as the source will be distributed in the resulting jar artifact preserving the same folder structure as in the source.

The following example shows how to set up a dependency with another artifact or sbt module containing the IDL definitions (`foo-domain`):

```
//...
.settings(
  Seq(
      publishMavenStyle := true,
      idlType := "avro",
      srcGenSerializationType := "AvroWithSchema",
      srcGenJarNames := Seq("foo-domain"),
      srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
      sourceGenerators in Compile += (Compile / srcGen).taskValue,
      libraryDependencies ++= Seq(
        "io.higherkindness" %% "mu-rpc-client-core" % V.muRPC
      )
  )
)
//...
```


[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/47deg/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
[avrohugger]: https://github.com/julianpeeters/avrohugger