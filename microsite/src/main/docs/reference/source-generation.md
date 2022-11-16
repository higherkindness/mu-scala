---
layout: docs
title: Source generation
section: reference
permalink: /reference/source-generation
---

# Source generation reference

This is a reference page for the `sbt-mu-srcgen` sbt plugin, which generates
Scala source code from Avro/Protobuf IDL files.

## Tasks

The sbt task used to perform source generation depends on your IDL type:

* For Avro, use the sbt-mu-srcgen plugin's `muSrcGen` task
* For Protobuf, use the sbt-protoc plugin's `protocGenerate` task
  (sbt-mu-srcgen automatically adds the sbt-protoc plugin to your project)

Alternatively you can just run the `compile` task, and the appropriate source
generation task will be executed before compilation.

## Settings

This section explains each of the sbt plugin's settings.  As a reminder, this
plugin needs to be manually enabled for any module for which you want to
generate code; you can do that by adding the following to your `build.sbt`:

```
enablePlugins(SrcGenPlugin)
```

### muSrcGenIdlType

The most important sbt setting is `muSrcGenIdlType`, which tells the plugin what
kind of IDL files (Avro/Protobuf) to look for.

```
muSrcGenIdlType := IdlType.Proto // or IdlType.Avro
```

### muSrcGenSerializationType

Another important setting is `muSrcGenSerializationType`, which specifies how
messages should be encoded on the wire. This should match the format you chose
for the `muSrcGenIdlType` setting:

* For Protobuf, there is no need to configure this setting, as there is only one
  possible serialization type
* For Avro, choose either `SerializationType.Avro` or `SerializationType.AvroWithSchema`.
    * If you choose `Avro`, it means your client and server must always use
      exactly the same version of the schema.
    * If you choose `AvroWithSchema`, the writer schema will be included in
      every message sent, which introduces a bandwidth overhead but allows
      schema evolution. In other words, the server and client can use different
      versions of a schema, as long as they are compatible with each other.  See
      the [schema evolution](schema-evolution/avro) section for more details on
      schema evolution.

```
muSrcGenSerializationType := SerializationType.Protobuf // or SerializationType.Avro or SerializationType.AvroWithSchema
```

### Other basic settings

| Setting | Description | Default value |
| --- | --- | --- |
| `muSrcGenSourceDirs` | The list of directories where your IDL files can be found.<br/><br/>Note: all the directories configured as sources will be distributed in the resulting jar artifact preserving the same folder structure as in the source. | `Compile / resourceDirectory`, typically `src/main/resources/` |
| `muSrcGenIdlTargetDir` | The directory where all discovered IDL files will be copied in preparation for Scala code generation. The plugin will automatically copy the following to the target directory:<br/> * All the IDL files and directories in the directory specified by `muSrcGenSourceDirs`<br/> * All the IDL files extracted from the JAR files or sbt modules specified by `muSrcGenJarNames` (see the "Advanced settings" section below) | `Compile / resourceManaged`, typically `target/scala-2.13/resource_managed/main` |
| `muSrcGenTargetDir` | The directory where the `muSrcGen` task will write the generated files. The files will be placed in subdirectories based on the namespaces declared in the IDL files. | `Compile / sourceManaged`, typically `target/scala-2.13/src_managed/main/` |

**Note**: The directories referenced in `muSrcGenSourceDirs` must exist. Target directories will be created upon generation.

### Advanced settings

| Setting | Description | Default value |
| --- | --- | --- |
| `muSrcGenJarNames` | A list of JAR file or sbt module names where extra IDL files can be found. See the [muSrcGenJarNames section](#musrcgenjarnames) section below for more details. | `Nil` |
| `muSrcGenIdlExtension` | The extension of IDL files to extract from JAR files or sbt modules. | * `avdl` if `muSrcGenIdlType` is `avro`<br/> * `proto` if `muSrcGenIdlType` is `Proto` |
| `muSrcGenCompressionType` | The compression type that will be used by generated RPC services. Set to `higherkindness.mu.rpc.srcgen.Model.GzipGen` for Gzip compression. | `higherkindness.mu.rpc.srcgen.Model.NoCompressionGen` |
| `muSrcGenIdiomaticEndpoints` | Flag indicating if idiomatic gRPC endpoints should be used. If `true`, the service operations will be prefixed by the namespace. | `true` |
| `muSrcGenProtocVersion` | Specifies the protoc version that [ScalaPB](https://scalapb.github.io/) should use when generating source files from proto files. | `None` (let ScalaPB choose the protoc version) |
| `muSrcGenValidateProto` | Flag indicating if the plugin should generate validation methods based on rules and constraints defined in the specs. Only proto is supported at this moment. | `false` |

### muSrcGenJarNames

You can use IDL files packaged into artifacts within your classpath, e.g. JAR
files added to the classpath via `libraryDependencies`, or other sbt modules.

`muSrcGenJarNames` can be very useful when you want to distribute your `IDL` files
without binary code (to prevent binary conflicts in clients).

The following example shows how to set up a dependency with another artifact or
sbt module containing the IDL definitions (`foo-domain`):

```
//...
.settings(
  Seq(
      muSrcGenIdlType := IdlType.Avro,
      muSrcGenSerializationType := SerializationType.AvroWithSchema,
      muSrcGenJarNames := Seq("foo-domain"),
      muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
      libraryDependencies ++= Seq(
        "io.higherkindness" %% "mu-rpc-service" % V.muRPC
      )
  )
)
//...
```

### muSrcGenValidateProto

The sbt-mu-srcgen supports the plugin [scalapb-validate](https://github.com/scalapb/scalapb-validate).
This plugin generates validators for your models, using the base validators defined by the [PGV protoc plugin](https://github.com/envoyproxy/protoc-gen-validate)

As you probably guessed, this is only compatible with proto and the setting will be ignored when working with Avro files.

To enable the validation methods generation, you need to set the setting `muSrcGenValidateProto` to true and
import the PVG validators provided transitively by the `scalapb-validate-core` protobuf library:

```
//...
.settings(
  Seq(
      muSrcGenIdlType := IdlType.Proto,
      muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_proto",
      muSrcGenValidateProto := true,
      libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf",
        "io.higherkindness" %% "mu-rpc-service" % V.muRPC
      )
  )
)
//...
```

## Implementation Notes: An Intentional Incompatibility with the Avro Standard

In order to make it easier for users to evolve their schemas over time,
`sbt-mu-srcgen` intentionally deviates from the Avro standard in one key way: it
restricts RPC return types to record types (`string sendUser(UserWithCountry
user)` is not permitted) as well as restricting the arguments of RPC messages to
_none_ or to a single record type (`SendUserResponse sendUser(UserWithCountry
user, RequestId id)` is not permitted).

If you attempt to write an Avro schema using primitive types instead of records
(for example, something like this):

```
@namespace("foo")
protocol UserV1 {
  string sendUser(string user);
}
```

the source generation command (i.e. `muSrcGen`) will fail with an error message
explaining why the protocol was rejected. For example, the above schema would trigger
the following message:

```
[error] (avro-protocol / Compile / muSrcGen) One or more IDL files are invalid. Error details:
[error]  /path/to/the/invalid/file.avdl has the following errors: RPC method request parameter 'user' has non-record request type 'STRING', RPC method response parameter has non-record response type 'STRING'
```

### Additional Context

To understand this potential issue with schema evolution, consider the following example,

```
record SearchRequest {
  string query;
}

SearchResponse search(SearchRequest request);
```

This schema can be evolved to add optional fields (e.g. ordering, filters, ...)
to the request. All the user has to do is just change the _single record_.

The following API design, on the other hand, can't be evolved because changing
the `query` argument from a `string` to any other datatype would introduce
backward incompatibility.

```
SearchResponse search(string query);
```

For this reason, we **enforce that all requests and responses must be records**.

The reason for disallowing multiple request arguments, on the other hand, is
that the gRPC spec does not support it. There is no obvious way to map multiple
arguments in the Avro RPC definition to a single gRPC request.

[Mu]: https://github.com/higherkindness/mu-scala
