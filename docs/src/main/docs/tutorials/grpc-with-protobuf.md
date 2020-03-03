---
layout: docs
title: gRPC with Protobuf
section: tutorials
permalink: tutorials/grpc-with-protobuf
---

# Tutorial: gRPC with Protobuf

This tutorial will show you how to generate a Mu RPC service definition from a
[Protocol Buffers] protocol file.

Then a [follow-up tutorial](grpc-server-client) will guide you through using
this service definition to create a fully working gRPC server or client.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have some understanding of Protobuf and `.proto` file syntax
* have read the [Getting Started guide](../getting-started)

This document will focus on Protobuf. If you would like to use gRPC with Avro,
see the [gRPC with Avro tutorial](grpc-with-avro).

## Create a new Mu project

As described in the [Getting Started guide](../getting-started), we recommend
you use the Mu-Scala giter8 template to create a new skeleton project. This will
install and configure the `mu-srcgen` sbt plugin, which we will need to generate
Scala code from a Protobuf `.proto` file.

## Write the Protobuf protocol

We're going to start by writing a `.proto` file containing our request and
response messages.

Copy the following `Protobuf` protocol and save it as
`src/main/resources/greeter.proto`:

```proto
syntax = "proto3";

package hello;

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
protoc-jar: executing: [/var/folders/33/gbkw7lt97l7b38jnzh49bwvh0000gn/T/protocjar8448157137886915423/bin/protoc.exe, --proto_path=/Users/chris/code/hello-mu-protobuf/target/scala-2.12/resource_managed/main/proto, --proto_path=/Users/chris/code/hello-mu-protobuf/target/scala-2.12/resource_managed/main/proto, --plugin=protoc-gen-proto2_to_proto3, --include_imports, --descriptor_set_out=greeter.proto.desc, greeter.proto]
[success] Total time: 0 s, completed 03-Mar-2020 11:23:35
```

Let's have a look at the code that Mu-Scala has generated. Open the file
`target/scala-2.12/src_managed/main/hello/greeter.scala` in your editor of
choice.

It's generated code, so it will look pretty ugly. Here's a version of it tidied
up a bit to make it more readable:

```scala
package hello

import higherkindness.mu.rpc.protocol._

object greeter {
  final case class HelloRequest(@pbIndex(1) name: String)
  final case class HelloResponse(@pbIndex(1) greeting: String, @pbIndex(2) happy: Boolean)
}
```

A few things to note:

* Mu-Scala has generated one case class for each Protobuf message
* The package name matches the one specified in the `.proto` file
* The case classes are inside an object whose name matches the filename of
  `greeter.proto`

## Add an RPC service

We now have some model classes to represent our RPC request and response, but we
don't have any RPC endpoints. Let's fix that by adding a service to the Protobuf
protocol.

Add the following lines at the end of `greeter.proto` to define an RPC service
with one endpoint:

```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloResponse);
}
```

## Regenerate the code

If you run the `muSrcGen` sbt task again, and inspect the
`target/scala-2.12/src_managed/main/hello/greeter.scala` file again, it should
look something like this:

```scala
package hello

import higherkindness.mu.rpc.protocol._

object greeter {
  final case class HelloRequest(@pbIndex(1) name: String)
  final case class HelloResponse(@pbIndex(1) greeting: String, @pbIndex(2) happy: Boolean)

  @service(Protobuf, Identity, namespace = Some("hello"), methodNameStyle = Capitalize)
  trait Greeter[F[_]] {
    def SayHello(req: HelloRequest): F[HelloResponse]
  }

}
```

A trait has been added to the generated code, corresponding to the `service` we
added to `greeter.proto`.

There's quite a lot going on there, so let's unpack it a bit.

* The trait is called `Greeter`, which matches the service name in the `.proto`
  file.
* The trait contains a method for each endpoint in the service.
* Mu-Scala uses "tagless final" encoding: the trait has a higher-kinded
  type parameter `F[_]` and all methods return their result wrapped in `F[...]`.
    * As we'll see in a later tutorial, `F[_]` becomes an IO monad such as
      [cats-effect] `IO` when we implement a gRPC server or client.
* The trait is annotated with `@service`. This is a macro annotation. When we
  compile the code, it will create a companion object for the trait and add a
  load of useful helper methods for creating servers and clients. We'll see how
  to make use of them in the next tutorial.
* The annotation has 4 parameters:
    1. `Protobuf` describes how gRPC requests and responses are serialized
    2. `Identity` means GZip compression of requests and responses is disabled
    3. `"hello"` is the namespace in which the RPC endpoint will be exposed
    4. `Capitalize` means the endpoint will be exposed as `SayHello`, not
       `sayHello`.

## Next steps

To find out how to turn this service definition into a working gRPC client or server, continue to the [gRPC server and client tutorial](grpc-server-client).

[cats-effect]: https://typelevel.org/cats-effect/
[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers
