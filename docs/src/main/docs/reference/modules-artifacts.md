---
layout: docs
title: Modules and artifacts
section: reference
permalink: reference/modules-artifacts
---

# Mu-Scala modules and artifacts

[Mu] is divided into multiple artifacts, grouped by scope:

* `Server`: specifically for RPC servers
* `Client`: specifically for RPC clients
* `Server/Client`: used from other artifacts for both Server and Client.
* `Test`: useful to test `Mu` applications.

## Common

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-common`  | Server/Client  | Provided*  | Common things that are used throughout the project.  |
| `mu-rpc-internal-core`  | Server/Client  | Provided*  | Macros.  |
| `mu-rpc-internal-monix`  | Server/Client  | Provided*  | Macros.  |
| `mu-rpc-internal-fs2`  | Server/Client  | Provided*  | Macros.  |

## RPC Client/Server

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-server`  | Server  | Yes  | Needed to attach RPC Services and spin-up an RPC Server. |
| `mu-rpc-channel`  | Server/Client  | Yes  | Mandatory to define protocols and auto-derived clients. |
| `mu-rpc-monix`  | Server/Client  | Yes  | Mandatory to define streaming operations with Monix Observables. |
| `mu-rpc-fs2`  | Server/Client  | Yes  | Mandatory to define streaming operations with fs2 Streams. |
| `mu-rpc-netty`  | Client  | Yes*  | `Netty` transport layer for the client. Mandatory if you need SSL/TLS support. |
| `mu-rpc-netty-ssl`  | Server/Client  | No  | Adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `mu-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. By adding this you wouldn't need to figure the right version, `mu-rpc` gives you the right one. |
| `mu-rpc-okhttp`  | Client  | Yes*  | `OkHttp` transport layer for the client. An alternative to `Netty`. |

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

## Metrics

| *Artifact Name*   | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-prometheus`  | Server/Client  | No  | Scala interceptors which can be used to monitor gRPC services using Prometheus.  |
| `mu-rpc-dropwizard`  | Server/Client  | No  | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics.  |

## Other

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-config`  | Server/Client  | No  | Provides configuration helpers using [pureconfig] to load the application configuration values.  |
| `mu-rpc-testing`  | Test  | No  | Utilities to test out `Mu` applications. It provides the `grpc-testing` library as the transitive dependency.  |
| `mu-rpc-client-cache`  | Client  | No  | Provides an algebra for caching RPC clients.  |
| `mu-rpc-marshallers-jodatime`  | Server/Client  | No  | Provides marshallers for serializing and deserializing jodatime `LocalDate` and `LocalDateTime` instances.  |

## Build
You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC server
libraryDependencies += "io.higherkindness" %% "mu-rpc-server" % "0.21.3"

// required for a protocol definition:
libraryDependencies += "io.higherkindness" %% "mu-rpc-channel" % "0.21.3"

// required for a protocol definition with streaming operations:
libraryDependencies += "io.higherkindness" %% "mu-rpc-monix" % "0.21.3"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-fs2" % "0.21.3"

// required for the use of generated RPC clients, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty" % "0.21.3"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-okhttp" % "0.21.3"

// optional - for easy RPC server/client configuration.
libraryDependencies += "io.higherkindness" %% "mu-config" % "0.21.3"

// optional - for RPC server/client metrics reporting, using Prometheus.
libraryDependencies += "io.higherkindness" %% "mu-rpc-prometheus" % "0.21.3"

// optional - for RPC server/client metrics reporting, using Dropwizard.
libraryDependencies += "io.higherkindness" %% "mu-rpc-dropwizard" % "0.21.3"

// optional - for communication between RPC server and client using SSL/TLS.
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty-ssl" % "0.21.3"

// optional - for RPC marshallers for jodatime types.
libraryDependencies += "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % "0.21.3"

// optional - to add caching support to RPC clients.
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-cache" % "0.21.3"

// optional - for testing RPC services
libraryDependencies += "io.higherkindness" %% "mu-rpc-testing" % "0.21.3" % Test
```

### sbt plugin

To generate Scala code from IDL files (`.proto`, `.avdl`, OpenAPI `.yaml`,
etc.), you will need the `sbt-mu-srcgen` plugin:

```
addSbtPlugin("io.higherkindness" %% "sbt-mu-srcgen" % "0.21.3")
```

[comment]: # (End Replace)



[Avro]: https://avro.apache.org/
[FS2]: https://github.com/functional-streams-for-scala/fs2
[gRPC]: https://grpc.io/
[Monix]: https://monix.io/
[Mu]: https://github.com/higherkindness/mu-scala
[OpenAPI]: https://swagger.io/docs/specification/about/
[Protobuf]: https://developers.google.com/protocol-buffers
[pureconfig]: https://github.com/pureconfig/pureconfig