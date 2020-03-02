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

## Outline

* Write a Protobuf protocol (`.proto`) file
* Generate classes
    * Set up the sbt plugin
    * Run `muSrcGen`
* Inspect the generated code
* Add an RPC service to the `.proto` file
* Run `muSrcGen` again
* Inspect the code again
* Explain the service annotation
* Link to next tutorial

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
}
```

## Generate Scala code


[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers
