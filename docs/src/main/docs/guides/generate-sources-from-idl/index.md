---
layout: docs
title: Generating sources from IDL
section: guides
permalink: /guides/generate-sources-from-idl
---

# Generate sources from IDL

While it is possible to use Mu by hand-writing your service definitions, message
classes and clients in Scala, we recommend you use `sbt-mu-srcgen` to generate
this code from Protobuf/Avro/OpenAPI IDL files.

IDL files are language-agnostic, more concise than Scala code, easily shared
with 3rd parties, and supported by a lot of existing tools.

Mu can generate code from a number of different IDL formats:

* message classes, gRPC server and client from Protobuf `.proto` files (see
  [Generating sources from Protocol Buffers](generate-sources-from-proto) for detailed instructions)
* message classes, gRPC server and client from Avro `.avpr` or `.avdl` files (see
  [Generating sources from Avro](generate-sources-from-avro))
* message classes and REST client from OpenAPI `.yaml` files (see the
  [OpenAPI REST client tutorial](../tutorials/openapi-client))

## Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "0.20.1")
```

[comment]: # (End Replace)

## How to use the plugin

The `muSrcGen` sbt task generates Scala source code from IDL files.

The plugin will automatically integrate the source generation into your compile
process, so the sources are generated before compilation when you run the
`compile` task.

You can also run the sbt task manually:

```sh
$ sbt muSrcGen
```

## Import

You will need to add this import at the top of your `build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._
```

## Settings

For an explanation of the plugin's settings, see the [source generation
reference](../reference/source-generation).

[Mu]: https://github.com/higherkindness/mu-scala
