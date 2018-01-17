
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/frees-io/freestyle-rpc.svg?branch=master)](https://travis-ci.org/frees-io/freestyle-rpc) [![codecov.io](http://codecov.io/github/frees-io/freestyle-rpc/coverage.svg?branch=master)](http://codecov.io/github/frees-io/freestyle-rpc?branch=master) [![Maven Central](https://img.shields.io/badge/maven%20central-0.9.0-green.svg)](https://oss.sonatype.org/#nexus-search;gav~io.frees~frees*) [![Latest version](https://img.shields.io/badge/freestyle--rpc-0.9.0-green.svg)](https://index.scala-lang.org/frees-io/freestyle-rpc) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/frees-io/freestyle-rpc/master/LICENSE) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/frees-io/freestyle-rpc.svg)](https://github.com/frees-io/freestyle-rpc/issues)

[comment]: # (End Badges)

# freestyle-rpc

Freestyle RPC is a purely functional library for building [RPC] endpoint-based services with support for [RPC] and [HTTP/2].

Also known as [frees-rpc], it brings the ability to combine [RPC] protocols, services, and clients in your `Freestyle` program, thanks to [gRPC].

## Installation

`frees-rpc` is cross-built for Scala `2.11.x` and `2.12.x`.

It's divided into multiple and different artifacts, grouped by scope:

* `Provided`: used from other artifacts and transitively provided.
* `Server`: specifically for the RPC server.
* `Client`: focused on the RPC auto-derived clients by `frees-rpc`.
* `Test`: useful to test `frees-rpc` applications.

*Artifact Name* | *Scope* | *Mandatory* | *Description*
--- | --- | --- | ---
`frees-rpc-server` | Server | Yes | Needed to attach RPC Services and spin-up an RPC Server.
`frees-rpc-client-core` | Client | Yes | Mandatory to define protocols and auto-derived clients.
`frees-rpc-client-netty` | Client | Yes* | Optional if you use `OkHttp`, required from the client perspective.
`frees-rpc-client-okhttp` | Client | Yes* | Optional if you use `Netty`, required from the client perspective.
`frees-rpc-config` | Server/Client | No | It provides configuration helpers using [frees-config] to load the application configuration values.
`frees-rpc-prometheus-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Server_ side.
`frees-rpc-prometheus-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Client_ side.
`frees-rpc-prometheus-shared` | Provided | No | Common code for both the client and the server in the prometheus scope.
`frees-rpc-dropwizard-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Server_ side.
`frees-rpc-dropwizard-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Client_ side.
`frees-rpc-interceptors` | Provided | No | Commons related to gRPC interceptors.
`frees-rpc-testing` | Test | No | Utilities to test out `frees-rpc` applications. It provides the `grpc-testing` library as the transitive dependency.
`frees-rpc-common` | Provided | Provided | Common things that are used throughout the project.
`frees-rpc-internal` | Provided | Provided | Macros.
`frees-rpc-async` | Provided | Provided | Async instances useful for interacting with the RPC services on both sides, server and the client.

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.

You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.frees" %% "frees-rpc-server"            % "0.9.0"

// required for a protocol definition:
libraryDependencies += "io.frees" %% "frees-rpc-client-core"       % "0.9.0"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.frees" %% "frees-rpc-client-netty"      % "0.9.0"
// or:
libraryDependencies += "io.frees" %% "frees-rpc-client-okhttp"     % "0.9.0"

// optional - for both server and client configuration.
libraryDependencies += "io.frees" %% "frees-rpc-config"            % "0.9.0"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-server" % "0.9.0"
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-client" % "0.9.0"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-server" % "0.9.0"
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-client" % "0.9.0"
```

[comment]: # (End Replace)

## Documentation

The full documentation is available at the [frees-rpc](http://frees.io/docs/rpc) site.

## Demo

See the [freestyle-rpc-examples](https://github.com/frees-io/freestyle-rpc-examples) repo.

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: http://frees.io/docs/rpc/
[frees-config]: http://frees.io/docs/patterns/config/

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017-2018 47 Degrees. <https://47deg.com>

[comment]: # (End Copyright)