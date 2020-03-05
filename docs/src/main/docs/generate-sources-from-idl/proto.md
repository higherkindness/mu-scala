---
layout: docs
title: Generating sources from Protobuf
permalink: /generate-sources-from-proto
---

# Generating sources from Protocol Buffers

## Getting started

First add the sbt plugin in `project/plugins.sbt`:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "0.20.1")
```

[comment]: # (End Replace)

Then configure the plugin by adding a few lines to `build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._

// Look for .proto files
muSrcGenIdlType := IdlType.Proto

// Make it easy for 3rd-party clients to communicate with our gRPC server
muSrcGenIdlGenIdiomaticEndpoints := true
```

Finally, make sure you have enabled the scalamacros compiler plugin so that
macro annotations work properly. Also in `build.sbt`:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
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

```sh
$ sbt muSrcGen
```

or as part of compilation:

```sh
$ sbt compile
```

Once the source generator has run, there should be a generated Scala file at
`target/scala-2.12/src_managed/main/foo/hello.scala`.

It will look roughly like this (tidied up and simplified for readability):

```scala
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

  @service(Protobuf, Identity, namespace = Some("foo"), methodNameStyle = Capitalize) trait ProtoGreeter[F[_]] {
    def SayHelloProto(req: HelloRequest): F[HelloResponse]
  }
}
```

Next, take a look at the [Patterns section](patterns) to see how you can
implement the `ProtoGreeter` trait and turn it into a gRPC server and client.
