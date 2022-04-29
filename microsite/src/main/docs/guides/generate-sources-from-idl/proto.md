---
layout: docs
title: Generating sources from Protobuf
section: guides
permalink: /guides/generate-sources-from-proto
---

# Generating sources from Protocol Buffers

## Getting started

First add the sbt plugin in `project/plugins.sbt`:

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "@VERSION@")
```

And enable the plugin on the appropriate project(s):

```scala
enablePlugins(SrcGenPlugin)
```

Once the plugin is enabled, you can configure it by adding a few lines to
`build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._

// Look for .proto files
muSrcGenIdlType := IdlType.Proto
```

Suppose you want to generate Scala code for a gRPC service based on the
following Protobuf IDL file, `src/main/resources/hello.proto`:

```proto
syntax = "proto3";

package foo;

message HelloRequest {
  string arg1 = 1;
  string arg2 = 2;
  repeated string arg3 = 3;
}

message HelloResponse {
  string arg1 = 1;
  string arg2 = 2;
  repeated string arg3 = 3;
}

service ProtoGreeter {
  rpc SayHelloProto (HelloRequest) returns (HelloResponse);
}
```

You can run the source generator directly:

```shell
sbt protocGenerate
```

or as part of compilation:

```shell
sbt compile
```

Once the source generator has run, there should be some generated Scala file
under `target/scala-2.13/src_managed/main/foo/hello/`.

There will be a separate file for each message class, plus a file for the
service definition.

`HelloRequest.scala` will look roughly like this (tidied up and simplified for
readability):

```scala
package foo.hello

final case class HelloRequest(
  arg1: String = "",
  arg2: String = "",
  arg3: Seq[String] = Seq.empty
) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[HelloRequest] {

  // ... lots of generated code

}
```

Note how each field has a default value, in line with the Protobuf spec.

The service definition in `ProtoGreeter.scala` will look something like this:

```scala
trait ProtoGreeter[F[_]] {
  def SayHelloProto(req: HelloRequest): F[HelloResponse]
}

object ProtoGreeter {

  // ... lots of generated code

}
```

## Custom types

ScalaPB allows you to customise what types are used in the generated code. For
example, if you have a `string` field in your Protobuf message that represents a
date, you might want to model it in Scala using `java.time.LocalDate`.

Take a look at the [ScalaPB
docs](https://scalapb.github.io/docs/customizations#custom-types) for details on
how to achieve this using ScalaPB's `TypeMapper` mechanism.

