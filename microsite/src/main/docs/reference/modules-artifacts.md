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

All of these artifacts are published for both Scala 2.13 and 3.x, except where noted
below.

## RPC Client/Server

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-service`  | Server/Client  | Yes  | Mandatory to build gRPC services and clients. |
| `mu-rpc-fs2`  | Server/Client  | Yes  | Mandatory to define streaming operations with FS2 Streams. |
| `mu-rpc-server`  | Server  | Yes  | Needed to attach RPC Services and spin-up an RPC Server. |
| `mu-rpc-client-netty`  | Client  | Yes*  | `Netty` transport layer for the client. Mandatory if you need SSL/TLS support. |
| `mu-rpc-client-okhttp`  | Client  | Yes*  | `OkHttp` transport layer for the client. An alternative to `Netty`. |
| `mu-rpc-netty-ssl`  | Server/Client  | No  | Adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `mu-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. By adding this you wouldn't need to figure the right version, `mu-rpc` gives you the right one. |

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.

## Metrics

| *Artifact Name*   | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-prometheus`  | Server/Client  | No  | Interceptors which can be used to monitor gRPC services using Prometheus.  |
| `mu-rpc-dropwizard`  | Server/Client  | No  | Interceptors which can be used to monitor gRPC services using Dropwizard metrics.  |

## Other

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-config`  | Server/Client  | No  | Provides configuration helpers using [pureconfig] to load the application configuration values. *Only available for Scala 2.13* because there is no Scala 3 build of pureconfig yet. |
| `mu-rpc-testing`  | Test  | No  | Utilities to test out `Mu` applications. It provides the `grpc-testing` library as the transitive dependency.  |
| `mu-rpc-client-cache`  | Client  | No  | Provides an algebra for caching RPC clients.  |

## Build
You can install any of these dependencies in your build as follows:

```
// required for a protocol definition:
libraryDependencies += "io.higherkindness" %% "mu-rpc-service" % "@VERSION@"

// required for a protocol definition with streaming operations:
libraryDependencies += "io.higherkindness" %% "mu-rpc-fs2" % "@VERSION@"

// required for the RPC server
libraryDependencies += "io.higherkindness" %% "mu-rpc-server" % "@VERSION@"

// required for the use of generated RPC clients, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-netty" % "@VERSION@"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-okhttp" % "@VERSION@"

// optional - for easy RPC server/client configuration.
libraryDependencies += "io.higherkindness" %% "mu-config" % "@VERSION@"

// optional - for RPC server/client metrics reporting, using Prometheus.
libraryDependencies += "io.higherkindness" %% "mu-rpc-prometheus" % "@VERSION@"

// optional - for RPC server/client metrics reporting, using Dropwizard.
libraryDependencies += "io.higherkindness" %% "mu-rpc-dropwizard" % "@VERSION@"

// optional - for communication between RPC server and client using SSL/TLS.
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty-ssl" % "@VERSION@"

// optional - to add caching support to RPC clients.
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-cache" % "@VERSION@"

// optional - for testing RPC services
libraryDependencies += "io.higherkindness" %% "mu-rpc-testing" % "@VERSION@" % Test
```

### sbt plugin

To generate Scala code from IDL files (`.proto`, `.avdl`, etc.), you will need the `sbt-mu-srcgen` plugin:

```
addSbtPlugin("io.higherkindness" %% "sbt-mu-srcgen" % "@VERSION@")
```

[Avro]: https://avro.apache.org/
[FS2]: https://github.com/functional-streams-for-scala/fs2
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu-scala
[Protobuf]: https://developers.google.com/protocol-buffers
[pureconfig]: https://github.com/pureconfig/pureconfig
