---
layout: docs
title: Getting Started
section: docs
permalink: /getting-started
---

# Getting Started

The easiest way to get started is to use our [giter8
template](https://github.com/higherkindness/mu-scala.g8) to start a new project:

```
sbt new higherkindness/mu-scala.g8
```
You can customise the template using a few parameters:

<a href="img/getting-started/sbt-new.png">
  <img src="img/getting-started/sbt-new.png" alt="Example of sbt new command" style="width: 100%; height: auto;">
</a>

The template will generate an sbt project with 3 modules:

* the protocol module, for generating source code from Avro/Protobuf IDL files
* the server module, for a gRPC server
* the client module, for a gRPC client

## Template parameters

There are a few important parameters to note.

### `create_sample_code`

If you set the `create_sample_code` parameter to `yes` (the default value), the
project will include sample code demonstrating how to build a gRPC server and
client with Mu:

* the `protocol` module will contain a "hello world" Avro/Protobuf IDL file
* the `client` module will contain a working implementation of a gRPC client
* the `server` module will contain a working implementation of a gRPC server, as
  well as a unit test

If you set `create_sample_code` to `no`, the three modules will still be
created, but they will be empty.

### `use_protobuf` and `use_avro`

You should set exactly one of these to `yes`, and the other to `no`.

Depending on these parameters, the example IDL file created in the `protocol`
module will be either a `.proto` or a `.avdl` file.

## Try it out

If you chose to create sample code, you can see everything working:

1. Start the server with `sbt server/run`
2. In another terminal window, run the client with `sbt client/run` and enter
   your name when prompted

You should see something like this:

<a href="img/getting-started/sbt-client-run.png">
  <img src="img/getting-started/sbt-client-run.png" alt="Example of running the client" style="width: 100%; height: auto;">
</a>

You can also run the unit test with `sbt server/test`.

## Next steps

Learn more about Mu-Scala concepts by following a [tutorial](tutorials).

The [gRPC with Protobuf](tutorials/grpc-with-protobuf) tutorial, or [gRPC with
Avro](tutorials/grpc-with-avro) if you prefer Avro, is a good place to start.
