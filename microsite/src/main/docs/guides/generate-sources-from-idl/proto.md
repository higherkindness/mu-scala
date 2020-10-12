---
layout: docs
title: Generating sources from Protobuf
section: guides
permalink: /guides/generate-sources-from-proto
---

# Generating sources from Protocol Buffers

## Getting started

First add the sbt plugin in `project/plugins.sbt`:

```sbt
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "@VERSION@")
```

**NOTE**

For users of the `sbt-mu-srcgen` plugin `v0.22.x` and below, the plugin is enabled automatically as soon as it's added to the `project/plugins.sbt`.
However, for users of the `sbt-mu-srcgen` plugin `v0.23.x` and beyond, the plugin needs to be manually enabled for any module for which you want to generate code.
To enable the module, add the following line to your `build.sbt`:

```sbt
enablePlugins(SrcGenPlugin)
```

Once the plugin is enabled, you can configure it by adding a few lines to `build.sbt`:

```sbt
import higherkindness.mu.rpc.srcgen.Model._

// Look for .proto files
muSrcGenIdlType := IdlType.Proto
```

Finally, make sure you have Scala macro annotations enabled, to ensure the
generated code compiles. How you do this depends on which Scala version you are
using.

For Scala 2.12, add this to `build.sbt`:

```sbt
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
```

For Scala 2.13, add this:

```sbt
scalacOptions += "-Ymacro-annotations"
```

Suppose you want to generate Scala code for a gRPC service based on the following Protobuf IDL file, `src/main/resources/hello.proto`:

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

```shell script
sbt muSrcGen
```

or as part of compilation:

```shell script
sbt compile
```

Once the source generator has run, there should be a generated Scala file at
`target/scala-2.13/src_managed/main/foo/hello.scala`.

It will look roughly like this (tidied up and simplified for readability):

```scala mdoc:silent
package foo

import higherkindness.mu.rpc.protocol._

object hello {
  final case class HelloRequest(
    arg1: String,
    arg2: String,
    arg3: List[String]
  )
  final case class HelloResponse(
    arg1: String,
    arg2: String,
    arg3: List[String]
  )

  @service(Protobuf, namespace = Some("foo")) trait ProtoGreeter[F[_]] {
    def SayHelloProto(req: HelloRequest): F[HelloResponse]
  }
}
```