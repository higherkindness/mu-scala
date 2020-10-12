---
layout: docs
title: gRPC with Protobuf
section: tutorials
permalink: tutorials/service-definition/protobuf
---

# Tutorial: RPC service definition with Protobuf

This tutorial will show you how to generate a Mu RPC service definition from a
[Protocol Buffers] protocol file.

Then a [follow-up tutorial](../grpc-server-client) will guide you through using
this service definition to create a fully working gRPC server or client.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have some understanding of Protobuf and `.proto` file syntax
* have read the [Getting Started guide](../../getting-started)

This document will focus on Protobuf. If you would like to use gRPC with Avro,
see the [RPC service definition with Avro tutorial](avro).

## Create a new Mu project

As described in the [Getting Started guide](../../getting-started), we recommend
you use the Mu-Scala [giter8
template](https://github.com/higherkindness/mu-scala.g8) to create a new
skeleton project. This will install and configure the `mu-srcgen` sbt plugin,
which we will need to generate Scala code from a Protobuf `.proto` file.

When you create the project using `sbt new`, make sure to set
`create_sample_code` to `no`. That way you can start with an empty project, and
gradually fill in the implementation as you follow the tutorial.

## Write the Protobuf protocol

We're going to start by writing a `.proto` file containing a couple of messages.
These messages will be used as the request and response types for a gRPC
endpoint later.

Copy the following Protobuf protocol and save it as
`protocol/src/main/resources/proto/hello.proto`:

```proto
syntax = "proto3";

package com.example;

message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string greeting = 1;
  bool happy = 2;
}
```

## Generate Scala code

Now we have a `.proto` file, we can generate Scala code from it.

Start sbt and run the `muSrcGen` task. You should see some Protobuf-related log
output:

```
sbt:hello-mu-protobuf> muSrcGen
protoc-jar: protoc version: 3.11.1, detected platform: osx-x86_64 (mac os x/x86_64)
protoc-jar: embedded: bin/3.11.1/protoc-3.11.1-osx-x86_64.exe
protoc-jar: executing: [/var/folders/33/gbkw7lt97l7b38jnzh49bwvh0000gn/T/protocjar11045051115974206116/bin/protoc.exe, --proto_path=/Users/chris/code/hello-mu-protobuf/protocol/target/scala-2.13/resource_managed/main/proto/proto, --proto_path=/Users/chris/code/hello-mu-protobuf/protocol/target/scala-2.13/resource_managed/main/proto, --plugin=protoc-gen-proto2_to_proto3, --include_imports, --descriptor_set_out=hello.proto.desc, hello.proto]
```

Let's have a look at the code that Mu-Scala has generated. Open the file
`protocol/target/scala-2.13/src_managed/main/com/example/hello.scala` in your
editor of choice.

It's generated code, so it will look pretty ugly. Here's a version of it tidied
up a bit to make it more readable:

```scala mdoc:silent
package com.example

import higherkindness.mu.rpc.protocol._

object hello {
  final case class HelloRequest(@pbIndex(1) name: String)
  final case class HelloResponse(@pbIndex(1) greeting: String, @pbIndex(2) happy: Boolean)
}
```

A few things to note:

* Mu-Scala has generated one case class for each Protobuf message
* The package name matches the one specified in the `.proto` file
* The case classes are inside an object whose name matches the filename of
  `hello.proto`

## Add an RPC service

We now have some model classes to represent our RPC request and response, but we
don't have any RPC endpoints. Let's fix that by adding a service to the Protobuf
protocol.

Add the following lines at the end of `hello.proto` to define an RPC service
with one endpoint:

```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloResponse);
}
```

## Regenerate the code

If you run the `muSrcGen` sbt task again, and inspect the
`protocol/target/scala-2.13/src_managed/main/com/example/hello.scala` file again, it should
look something like this:

```scala mdoc:silent
package com.example

import higherkindness.mu.rpc.protocol._

object hello {
  final case class HelloRequest(@pbIndex(1) name: String)
  final case class HelloResponse(@pbIndex(1) greeting: String, @pbIndex(2) happy: Boolean)

  @service(Protobuf, namespace = Some("com.example"))
  trait Greeter[F[_]] {
    def SayHello(req: HelloRequest): F[HelloResponse]
  }

}
```

A trait has been added to the generated code, corresponding to the `service` we
added to `hello.proto`.

There's quite a lot going on there, so let's unpack it a bit.

* The trait is called `Greeter`, which matches the service name in the `.proto`
  file.
* The trait contains a method for each endpoint in the service.
* Mu-Scala uses "tagless final" encoding: the trait has a higher-kinded
  type parameter `F[_]` and all methods return their result wrapped in `F[...]`.
    * As we'll see in a later tutorial, `F[_]` becomes an IO monad such as
      [cats-effect] `IO` when we implement a gRPC server or client.
* The trait is annotated with `@service`. This is a macro annotation. When we
  compile the code, it will create a companion object for the trait containing a
  load of useful helper methods for creating servers and clients. We'll see how
  to make use of these helpers in the next tutorial.
* The annotation has 3 parameters:
    1. `Protobuf` describes how gRPC requests and responses are serialized
    2. `Identity` means GZip compression of requests and responses is disabled
    3. `"com.example"` is the namespace in which the RPC endpoint will be exposed

These parameters can be customised using sbt settings. Take a look at the
[source generation reference](../../reference/source-generation) for more
details.

## Next steps

To find out how to turn this service definition into a working gRPC client or
server, continue to the [gRPC server and client tutorial](../grpc-server-client).

[cats-effect]: https://typelevel.org/cats-effect/
[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers
