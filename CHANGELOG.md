# Changelog

## 10/09/2017 - Version 0.0.5

Release changes:

* Upgrades to sbt 1.0.1 and Scala 2.12.3 ([#48](https://github.com/frees-io/freestyle-rpc/pull/48))
* Brings sbt-frees-protogen as a separate Artifact ([#49](https://github.com/frees-io/freestyle-rpc/pull/49))
* Adds warning about generated proto files ([#50](https://github.com/frees-io/freestyle-rpc/pull/50))
* Fixes Travis Builds ([#52](https://github.com/frees-io/freestyle-rpc/pull/52))
* Fixes RPC build and Publishing Issues ([#53](https://github.com/frees-io/freestyle-rpc/pull/53))


## 10/03/2017 - Version 0.0.3

Release changes:

* Makes the ChannelBuilder build a public method ([#45](https://github.com/frees-io/freestyle-rpc/pull/45))
* Fixes Client Streaming rpc server ([#46](https://github.com/frees-io/freestyle-rpc/pull/46))


## 09/08/2017 - Version 0.0.2

Release changes:

* Bug Fix  Proto Code Generator for Custom Types ([#42](https://github.com/frees-io/freestyle-rpc/pull/42))
* Fixes proto code generator for repeated types ([#43](https://github.com/frees-io/freestyle-rpc/pull/43))
* Adds LoggingM as a part of GrpcServerApp module ([#44](https://github.com/frees-io/freestyle-rpc/pull/44))


## 09/05/2017 - Version 0.0.1

Release changes:

* Migrates from mezzo to freestyle-rpc style, license, etc. ([#4](https://github.com/frees-io/freestyle-rpc/pull/4))
* Adds a dummy grpc demo for testing purposes ([#5](https://github.com/frees-io/freestyle-rpc/pull/5))
* gRPC extended Demos ([#6](https://github.com/frees-io/freestyle-rpc/pull/6))
* grpc-gateway Demo ([#7](https://github.com/frees-io/freestyle-rpc/pull/7))
* Divides demo projects in two different sbt modules ([#8](https://github.com/frees-io/freestyle-rpc/pull/8))
* Provides grpc configuration DSL and GrpcServer algebras ([#13](https://github.com/frees-io/freestyle-rpc/pull/13))
* Provides a Demo Extension ([#14](https://github.com/frees-io/freestyle-rpc/pull/14))
* Client Definitions based on free algebras - Unary Services  ([#16](https://github.com/frees-io/freestyle-rpc/pull/16))
* Migrates to sbt-freestyle 0.1.0 ([#19](https://github.com/frees-io/freestyle-rpc/pull/19))
* Server/Channel Configuration ([#20](https://github.com/frees-io/freestyle-rpc/pull/20))
* Server Definitions - Test Coverage ([#22](https://github.com/frees-io/freestyle-rpc/pull/22))
* Adds additional server definitions tests ([#23](https://github.com/frees-io/freestyle-rpc/pull/23))
* Generate .proto files from Freestyle service protocols ([#12](https://github.com/frees-io/freestyle-rpc/pull/12))
* Adds tests for some client handlers ([#27](https://github.com/frees-io/freestyle-rpc/pull/27))
* @service Macro ([#31](https://github.com/frees-io/freestyle-rpc/pull/31))
* RPC Client macro definitions ([#32](https://github.com/frees-io/freestyle-rpc/pull/32))
* monix.reactive.Observable for Streaming Services API ([#33](https://github.com/frees-io/freestyle-rpc/pull/33))
* Completes the basic Example ([#36](https://github.com/frees-io/freestyle-rpc/pull/36))
* Minor fix ([#35](https://github.com/frees-io/freestyle-rpc/pull/35))
* Renaming to frees-rpc. Moves examples to its own repository ([#40](https://github.com/frees-io/freestyle-rpc/pull/40))
* Upgrades gRPC. Releases frees-rpc 0.0.1. ([#41](https://github.com/frees-io/freestyle-rpc/pull/41))