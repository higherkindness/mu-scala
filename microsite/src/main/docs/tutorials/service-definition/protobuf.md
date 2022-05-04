---
layout: docs
title: RPC service definition with Protobuf
section: tutorials
permalink: tutorials/service-definition/protobuf
---

# Tutorial: RPC service definition with Protobuf

This tutorial will show you how to generate a Mu RPC service definition from a
[Protocol Buffers] protocol file.

Then a [follow-up tutorial](../grpc-server-client) will guide you through using
this service definition to create a fully working gRPC server or client.

This tutorial is aimed at developers who:

- are new to Mu-Scala
- have some understanding of Protobuf and `.proto` file syntax
- have read the [Getting Started guide](../../getting-started)

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
`protocol/src/main/resources/greeter.proto`:

```proto
syntax = "proto3";

package mu.examples.protobuf;

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

Start sbt and run the `compile` task. This will trigger the sbt-mu-srcgen plugin
to generate some Scala source files from the `.proto` file, and then those Scala
files will be compiled.

```
sbt:hello-mu-protobuf> compile
[info] Compiling 1 protobuf files to /Users/chris/code/hello-mu-protobuf/protocol/target/scala-2.13/src_managed/main
[info] compiling 3 Scala sources to /Users/chris/code/hello-mu-protobuf/protocol/target/scala-2.13/classes ...
[success] Total time: 2 s, completed 28 Apr 2022, 12:08:49
```

Let's have a look at the code that was generated. Open the file
`protocol/target/scala-2.13/src_managed/main/mu/examples/protobuf/greeter/HelloRequest.scala`
in your editor of choice.

It's generated code, so it will look pretty ugly. Here's a version of it tidied
up a bit to make it more readable:

```scala
package mu.examples.protobuf.greeter

final case class HelloRequest(
    name: String = "",
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
) extends scalapb.GeneratedMessage {

  // ... lots of generated code

}
```

A few things to note:

- Mu-Scala has generated one case class for each Protobuf message
- The package name is derived from the protobuf package declaration in the
  `.proto` file (`mu.examples.protobuf`) and the filename (`greeter.proto`)
  `hello.proto`
- The case class extends `scalapb.GeneratedMessage`. The sbt-mu-srcgen plugin
  delegates generation of code from `.proto` files to another plugin called
  [ScalaPB].

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

If you run the `compile` sbt task again, and inspect the
`protocol/target/scala-2.13/src_managed/main/mu/examples/greeter/Greeter.scala`
file, it should look something like this:

```scala
trait Greeter[F[_]] {
  def SayHello(req: HelloRequest): F[HelloResponse]
}

object Greeter {
  // ... lots of generated code
}
```

A trait has been added to the generated code, corresponding to the `service` we
added to `hello.proto`.

There's quite a lot going on there, so let's unpack it a bit.

- The trait is called `Greeter`, which matches the service name in the `.proto`
  file.
- The trait contains a method for each endpoint in the service.
- Mu-Scala uses "tagless final" encoding: the trait has a higher-kinded
  type parameter `F[_]` and all methods return their result wrapped in `F[...]`.
  - As we'll see in a later tutorial, `F[_]` becomes an IO monad such as
    [cats-effect] `IO` when we implement a gRPC server or client.
- There is also a companion object containing a load of useful helper methods
  for creating servers and clients. We'll see how to make use of these helpers
  in the next tutorial.

For details on how to customise this generated code using sbt settings, take a
look at the [source generation reference](../../reference/source-generation).

## Next steps

To find out how to turn this service definition into a working gRPC client or
server, continue to the [gRPC server and client tutorial](../grpc-server-client).

[cats-effect]: https://typelevel.org/cats-effect/
[grpc]: https://grpc.io/
[protocol buffers]: https://developers.google.com/protocol-buffers
[ScalaPB]: https://scalapb.github.io/
