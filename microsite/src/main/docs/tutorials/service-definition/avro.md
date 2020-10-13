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
you use the Mu-Scala [giter8 template](https://github.com/higherkindness/mu-scala.g8) to create a new
skeleton project. This will install and configure the `mu-srcgen` sbt plugin,
which we will need to generate Scala code from an Avro `.avdl` file.

When you create the project using `sbt new`, make sure to set
`create_sample_code` to `no`. That way you can start with an empty project, and
gradually fill in the implementation as you follow the tutorial.

You should also set `use_proto` to `no`, and `use_avro` to `yes`. This will
ensure the sbt project is correctly configured to generate code from Avro IDL
files.

## Write the IDL file

We're going to start by writing a `.avdl` file containing a couple of messages.
These messages will be used as the request and response types for a gRPC
endpoint later.

Copy the following Avro IDL and save it as `protocol/src/main/resources/avro/hello.avdl`:

```avroidl
@namespace("com.example")
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
`protocol/target/scala-2.13/src_managed/main/com/example/Greeter.scala` in your
editor of choice.

It should look something like this:

```scala
package com.example

import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._

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

Add the following line to `hello.avro` to define an RPC endpoint:

```avroidl
HelloResponse SayHello(HelloRequest request);
```

Make sure that you add the line inside the `protocol` block, before the closing
curly brace.

## Regenerate the code

If you run the `muSrcGen` sbt task again, and inspect the
`protocol/target/scala-2.13/src_managed/main/com/example/Greeter.scala` file
again, it should look something like this:

```scala
package com.example

import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._

final case class HelloRequest(name: String)

final case class HelloResponse(greeting: String, happy: Boolean)

@service(Avro, compressionType = Identity, namespace = Some("com.example"))
trait Greeter[F[_]] {
  def SayHello(request: com.example.HelloRequest): F[com.example.HelloResponse]
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
* The trait is annotated with `@service`. This is a macro annotation. When we
  compile the code, it will create a companion object for the trait containing a
  load of useful helper methods for creating servers and clients. We'll see how
  to make use of these helpers in the next tutorial.
* The annotation has 3 parameters:
    1. `Avro` describes how gRPC requests and responses are serialized
    2. `Identity` means GZip compression of requests and responses is disabled
    3. `"com.example"` is the namespace in which the RPC endpoint will be exposed
       
These parameters can be customised using sbt settings. Take a look at the
[source generation reference](../../reference/source-generation) for more details.

## Next steps

To find out how to turn this service definition into a working gRPC client or
server, continue to the [gRPC server and client tutorial](../grpc-server-client).

[Avro]: https://avro.apache.org/docs/current/
[cats-effect]: https://typelevel.org/cats-effect/
[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers
