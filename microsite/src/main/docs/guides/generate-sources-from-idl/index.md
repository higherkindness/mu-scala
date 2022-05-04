---
layout: docs
title: Generating sources from IDL
section: guides
permalink: /guides/generate-sources-from-idl
---

# Generate sources from IDL

Mu can generate code from a number of different IDL formats:

* message classes, gRPC server and client from Protobuf `.proto` files (see
  [Generating sources from Protocol Buffers](generate-sources-from-proto) for
  detailed instructions)
* message classes, gRPC server and client from Avro `.avpr` or `.avdl` files
  (see [Generating sources from Avro](generate-sources-from-avro))

## Plugin Installation

Add the following line to `project/plugins.sbt`:

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "@VERSION@")
```

And enable the plugin on the appropriate project(s):

```scala
enablePlugins(SrcGenPlugin)
```

## How to use the plugin

The plugin will automatically integrate its source generation into your compile
process, so the sources are generated before compilation when you run the
`compile` task.

You can also run the sbt task manually. To generate code from Avro IDL files:

```shell
sbt muSrcGen
```

Or for Protobuf:

```shell
sbt protocGenerate
```

## Import

If you want to customize the plugin's configuration, you will need to add this
import at the top of your `build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._
```

## Settings

For an explanation of the plugin's settings, see the [source generation reference](../reference/source-generation).
