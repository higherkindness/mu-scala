---
layout: home
title: Home
technologies:
 - first: ["Scala", "Frees-rpc library is completely written in Scala."]
 - second: ["gRPC", "Frees-rpc combines RPC protocols, services and clients thank to gRPC framework."]
 - third: ["Functional Programming", "frees-rpc is a purely functional library for building RPC endpoint based services."]
---

# Quickstart

## What’s frees-rpc

[frees-rpc] provides the ability to combine [RPC] protocols, services, and clients in your `Freestyle` program, thanks to [gRPC]. Although it's fully integrated with [gRPC], there are some important differences when defining the protocols, as we’ll see later on, since [frees-rpc] follows the same philosophy as `Freestyle` core, being macro-powered.

## Installation

`frees-rpc` is cross-built for Scala `2.11.x` and `2.12.x`.

It's divided into multiple and different artifacts, grouped by scope:

* `Server`: specifically for the RPC server.
* `Client`: focused on the RPC auto-derived clients by `frees-rpc`.
* `Server/Client`: used from other artifacts for both Server and Client.
* `Test`: useful to test `frees-rpc` applications.

*Artifact Name* | *Scope* | *Mandatory* | *Description*
--- | --- | --- | ---
`frees-rpc-server` | Server | Yes | Needed to attach RPC Services and spin-up an RPC Server.
`frees-rpc-client-core` | Client | Yes | Mandatory to define protocols and auto-derived clients.
`frees-rpc-client-netty` | Client | Yes* | Mandatory on the client side if we are using `Netty` in the server side.
`frees-rpc-client-okhttp` | Client | Yes* | Mandatory on the client side if we are using `OkHttp` in the server side.
`frees-rpc-config` | Server/Client | No | Provides configuration helpers using [frees-config] to load the application configuration values.
`frees-rpc-marshallers-jodatime` | Server/Client | No | Provides marshallers for serializing and deserializing the `LocalDate` and `LocalDateTime` joda instances.
`frees-rpc-prometheus-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Server_ side.
`frees-rpc-prometheus-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Client_ side.
`frees-rpc-prometheus-shared` | Server/Client | No | Common code for both the client and the server in the prometheus scope.
`frees-rpc-dropwizard-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Server_ side.
`frees-rpc-dropwizard-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Client_ side.
`frees-rpc-interceptors` | Server/Client | No | Commons related to gRPC interceptors.
`frees-rpc-testing` | Test | No | Utilities to test out `frees-rpc` applications. It provides the `grpc-testing` library as the transitive dependency.
`frees-rpc-common` | Server/Client | Provided* | Common things that are used throughout the project.
`frees-rpc-internal` | Server/Client | Provided* | Macros.
`frees-rpc-async` | Server/Client | Provided* | Async instances useful for interacting with the RPC services on both sides, server and the client.
`frees-rpc-netty-ssl` | Server/Client | No | Adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `frees-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. Adding this you wouldn't need to figure out which would be the right version, `frees-rpc` gives you the right one.

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.frees" %% "frees-rpc-server"            % "0.15.1"

// required for a protocol definition:
libraryDependencies += "io.frees" %% "frees-rpc-client-core"       % "0.15.1"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.frees" %% "frees-rpc-client-netty"      % "0.15.1"
// or:
libraryDependencies += "io.frees" %% "frees-rpc-client-okhttp"     % "0.15.1"

// optional - for both server and client configuration.
libraryDependencies += "io.frees" %% "frees-rpc-config"            % "0.15.1"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-server" % "0.15.1"
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-client" % "0.15.1"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-server" % "0.15.1"
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-client" % "0.15.1"

// optional - for the communication between server and client by using SSL/TLS.
libraryDependencies += "io.frees" %% "frees-rpc-netty-ssl" % "0.15.1"

// optional - for using the jodatime marshallers.
libraryDependencies += "io.frees" %% "frees-rpc-marshallers-jodatime" % "0.15.1"
```

[comment]: # (End Replace)

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: https://github.com/frees-io/freestyle-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier