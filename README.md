
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/frees-io/freestyle-rpc.svg?branch=master)](https://travis-ci.org/frees-io/freestyle-rpc) [![codecov.io](http://codecov.io/github/frees-io/freestyle-rpc/coverage.svg?branch=master)](http://codecov.io/github/frees-io/freestyle-rpc?branch=master) [![Maven Central](https://img.shields.io/badge/maven%20central-0.9.0-green.svg)](https://oss.sonatype.org/#nexus-search;gav~io.frees~frees*) [![Latest version](https://img.shields.io/badge/freestyle--rpc-0.9.0-green.svg)](https://index.scala-lang.org/frees-io/freestyle-rpc) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/frees-io/freestyle-rpc/master/LICENSE) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/frees-io/freestyle-rpc.svg)](https://github.com/frees-io/freestyle-rpc/issues)

[comment]: # (End Badges)

# freestyle-rpc

Freestyle RPC is a purely functional library for building [RPC] endpoint based services with support for [RPC] and [HTTP/2].

Also known as [frees-rpc], it brings the ability to combine [RPC] protocols, services and clients in your `Freestyle` program, thanks to [gRPC].

## Installation

`frees-rpc` is cross-built for Scala `2.11.x` and `2.12.x`:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.frees" %% "frees-rpc-server"        % "0.9.0"

// required for a protocol definition:
libraryDependencies += "io.frees" %% "frees-rpc-client-core"   % "0.9.0"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.frees" %% "frees-rpc-client-netty"  % "0.9.0"
// or:
libraryDependencies += "io.frees" %% "frees-rpc-client-okhttp" % "0.9.0"

// optional - for both server and client configuration.
libraryDependencies += "io.frees" %% "frees-rpc-config"        % "0.9.0"
```

[comment]: # (End Replace)

Note: `frees-rpc-config` provides some configuration helpers using [frees-config] to load the application configuration values.

## Documentation

The full documentation is available at [frees-rpc](http://frees.io/docs/rpc) site.

## Sbt Modules

`frees-rpc` code is placed in different sbt modules:

* `frees-rpc-common`: contains the protocol types, with the minimum set of dependencies.
* `frees-rpc-async`: contains just the async implicit instances (NTs between effect/async types).
* `frees-rpc-internal` where the macros are placed.
* `frees-rpc-client-core`: algebra and code related to the RPC clients.
* `frees-rpc-client-netty`: it doesn't add any additional code, just a transport layer provider based on `grpc-netty`.
* `frees-rpc-client-okhttp`: similar to the `Netty` one, it doesn't add any additional code, just a transport layer provider based on `grpc-okhttp`.
* `frees-rpc-server`: algebra and code related to the RPC server.
* `frees-rpc-config`: helpers to be able to load the client and the server configuration.

## Demo

See [freestyle-rpc-examples](https://github.com/frees-io/freestyle-rpc-examples) repo.

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: http://frees.io/docs/rpc/
[frees-config]: http://frees.io/docs/patterns/config/

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017-2018 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)