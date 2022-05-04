---
layout: docs
title: RPC service definition with Avro
section: tutorials
permalink: tutorials/service-definition/avro
---

# Tutorial: RPC service definition with Avro

[gRPC] supports [Protocol Buffers] by default for serialization of requests and
responses, but it also allows you to use other serialization mechanisms,
including [Avro].

This tutorial will show you how to generate a Mu RPC service definition from an
[Avro] IDL file.

Then a [follow-up tutorial](../grpc-server-client) will guide you through using
this service definition to create a fully working gRPC server or client.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have some understanding of [Avro] and `.avdl` ([Avro IDL](https://avro.apache.org/docs/current/idl.html)) syntax
* have read the [Getting Started guide](../../getting-started)

This document will focus on Avro. If you would like to use gRPC with Protobuf,
see the [RPC service definition with Protobuf tutorial](protobuf).

## Create a new Mu project

As described in the [Getting Started guide](../../getting-started), we recommend
you use the Mu-Scala [giter8
template](https://github.com/higherkindness/mu-scala.g8) to create a new
skeleton project. This will install and configure the `mu-srcgen` sbt plugin,
which we will need to generate Scala code from an Avro `.avdl` file.

When you create the project using `sbt new`, make sure to set
`create_sample_code` to `no`. That way you can start with an empty project, and
gradually fill in the implementation as you follow the tutorial.

You should also set `use_protobuf` to `no`, and `use_avro` to `yes`. This will
ensure the sbt project is correctly configured to generate code from Avro IDL
files.

## Write the IDL file

We're going to start by writing a `.avdl` file containing a couple of messages.
These messages will be used as the request and response types for a gRPC
endpoint later.

Copy the following Avro IDL and save it as
`protocol/src/main/resources/avro/greeter.avdl`:

```
@namespace("mu.examples.avro")
protocol Greeter {

  record HelloRequest {
    string name;
  }

  record HelloResponse {
    string greeting;
    boolean happy;
  }

}
```

## Generate Scala code

Now we have a `.avdl` file, we can generate Scala code from it.

Start sbt and run the `muSrcGen` task. This will discover the `.avdl` file,
parse it and generate corresponding Scala code.

Let's have a look at the code that Mu-Scala has generated. Open the file
`protocol/target/scala-2.13/src_managed/main/mu/examples/avro/Greeter.scala` in
your editor of choice.

It should look something like this:

```scala
package mu.examples.avro

final case class HelloRequest(name: String)

final case class HelloResponse(greeting: String, happy: Boolean)
```

A few things to note:

* Mu-Scala has generated one case class for each Avro record
* The package name matches the namespace specified in the `.avdl` file

## Add an RPC service

We now have some model classes to represent our RPC request and response, but we
don't have any RPC endpoints. Let's fix that by adding a method to the Avro
protocol.

Add the following line to `greeter.avdl` to define an RPC endpoint:

```
HelloResponse SayHello(HelloRequest request);
```

Make sure that you add the line inside the `protocol` block, before the closing
curly brace.

## Regenerate the code

If you run the `muSrcGen` sbt task again, and inspect the
`protocol/target/scala-2.13/src_managed/main/mu/examples/avro/Greeter.scala`
file again, it should look something like this:

```scala
package mu.examples.avro

final case class HelloRequest(name: String)

final case class HelloResponse(greeting: String, happy: Boolean)

trait Greeter[F[_]] {
  def SayHello(request: mu.examples.avro.HelloRequest): F[mu.examples.avro.HelloResponse]
}

object Greeter {

  // ... lots of generated code

}
```

Now that our `.avdl` file has at least one method, we have an RPC service
definition, so a corresponding trait has been added to the generated code.

There's quite a lot going on there, so let's unpack it a bit.

* The trait is called `Greeter`, which matches the protocol name in the `.avdl` file.
* The trait contains a method for each endpoint in the service.
* Mu-Scala uses "tagless final" encoding: the trait has a higher-kinded
  type parameter `F[_]` and all methods return their result wrapped in `F[...]`.
    * As we'll see in a later tutorial, `F[_]` becomes an IO monad such as
      [cats-effect] `IO` when we implement a gRPC server or client.

For details on how to customise this generated code using sbt settings, take a
look at the [source generation reference](../../reference/source-generation).

## Next steps

To find out how to turn this service definition into a working gRPC client or
server, continue to the [gRPC server and client tutorial](../grpc-server-client).

[Avro]: https://avro.apache.org/docs/current/
[cats-effect]: https://typelevel.org/cats-effect/
[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers
