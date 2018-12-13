
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/higherkindness/mu.svg?branch=master)](https://travis-ci.org/higherkindness/mu) [![codecov.io](http://codecov.io/gh/higherkindness/mu/branch/master/graph/badge.svg)](http://codecov.io/gh/higherkindness/mu) [![Maven Central](https://img.shields.io/badge/maven%20central-0.16.0-green.svg)](https://oss.sonatype.org/#nexus-search;gav~io.higherkindness~mu*) [![Latest version](https://img.shields.io/badge/mu-0.16.0-green.svg)](https://index.scala-lang.org/higherkindness/mu) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/higherkindness/mu/master/LICENSE) [![Join the chat at https://gitter.im/47deg/mu](https://badges.gitter.im/47deg/mu.svg)](https://gitter.im/47deg/mu?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/higherkindness/mu.svg)](https://github.com/higherkindness/mu/issues)

[comment]: # (End Badges)

# mu-rpc

Mu RPC is a purely functional library for building [RPC] endpoint-based services with support for [RPC] and [HTTP/2].

Also known as [mu], it brings the ability to combine [RPC] protocols, services, and clients in your `Freestyle` program, thanks to [gRPC].

## Installation

`mu-rpc` is cross-built for Scala `2.11.x` and `2.12.x`.

It's divided into multiple and different artifacts, grouped by scope:

* `Server`: specifically for the RPC server.
* `Client`: focused on the RPC auto-derived clients by `mu-rpc`.
* `Server/Client`: used from other artifacts for both Server and Client.
* `Test`: useful to test `mu-rpc` applications.

*Artifact Name* | *Scope* | *Mandatory* | *Description*
--- | --- | --- | ---
`mu-rpc-server` | Server | Yes | Needed to attach RPC Services and spin-up an RPC Server.
`mu-rpc-client-core` | Client | Yes | Mandatory to define protocols and auto-derived clients.
`mu-rpc-client-netty` | Client | Yes* | Mandatory on the client side if we are using `Netty` in the server side.
`mu-rpc-client-okhttp` | Client | Yes* | Mandatory on the client side if we are using `OkHttp` in the server side.
`mu-config` | Server/Client | No | Provides configuration helpers using [mu-config] to load the application configuration values.
`mu-rpc-marshallers-jodatime` | Server/Client | No | Provides marshallers for serializing and deserializing the `LocalDate` and `LocalDateTime` joda instances.
`mu-rpc-prometheus-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Server_ side.
`mu-rpc-prometheus-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Client_ side.
`mu-rpc-prometheus-shared` | Server/Client | No | Common code for both the client and the server in the prometheus scope.
`mu-rpc-dropwizard-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Server_ side.
`mu-rpc-dropwizard-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Client_ side.
`mu-rpc-interceptors` | Server/Client | No | Commons related to gRPC interceptors.
`mu-rpc-testing` | Test | No | Utilities to test out `mu-rpc` applications. It provides the `grpc-testing` library as the transitive dependency.
`mu-common` | Server/Client | Provided* | Common things that are used throughout the project.
`mu-rpc-internal` | Server/Client | Provided* | Macros.
`mu-rpc-async` | Server/Client | Provided* | Async instances useful for interacting with the RPC services on both sides, server and the client.
`mu-rpc-netty-ssl` | Server/Client | No | Adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `mu-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. Adding this you wouldn't need to figure out which would be the right version, `mu-rpc` gives you the right one.

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

To use the project, add the following to your build.sbt:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
```

You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.higherkindness" %% "mu-rpc-server"            % "0.16.0"

// required for a protocol definition:
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-core"       % "0.16.0"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-netty"      % "0.16.0"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-okhttp"     % "0.16.0"

// optional - for both server and client configuration.
libraryDependencies += "io.higherkindness" %% "mu-config"                % "0.16.0"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.higherkindness" %% "mu-rpc-prometheus-server" % "0.16.0"
libraryDependencies += "io.higherkindness" %% "mu-rpc-prometheus-client" % "0.16.0"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.higherkindness" %% "mu-rpc-dropwizard-server" % "0.16.0"
libraryDependencies += "io.higherkindness" %% "mu-rpc-dropwizard-client" % "0.16.0"

// optional - for the communication between server and client by using SSL/TLS.
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty-ssl" % "0.16.0"

// optional - for using the jodatime marshallers.
libraryDependencies += "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % "0.16.0"
```

[comment]: # (End Replace)

## Documentation

The full documentation is available at the [mu](https://higherkindness.github.io/mu) site.

## Demo

See the [examples](/modules/examples) module.

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu]: https://higherkindness.github.io/mu/
[frees-config]: http://frees.io/docs/patterns/config/

[comment]: # (Start Copyright)
# Copyright

mu is designed and developed by 47 Degrees

Copyright (C) 2017-2018 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)