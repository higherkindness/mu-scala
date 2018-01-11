# Changelog

## 01/11/2018 - Version 0.8.0

Release changes:

* Adds the job in Travis for the after CI SBT task ([#116](https://github.com/frees-io/freestyle-rpc/pull/116))
* frees-rpc Tagless-final Migration - Release 0.8.0 ([#117](https://github.com/frees-io/freestyle-rpc/pull/117))


## 01/10/2018 - Version 0.7.0

Release changes:

* Update build ([#108](https://github.com/frees-io/freestyle-rpc/pull/108))
* Splits core module in [core, config] ([#109](https://github.com/frees-io/freestyle-rpc/pull/109))
* Organizes all sbt modules under modules folder ([#112](https://github.com/frees-io/freestyle-rpc/pull/112))
* Splits core into Server and Client submodules ([#113](https://github.com/frees-io/freestyle-rpc/pull/113))
* Moves non-server tests to the root ([#114](https://github.com/frees-io/freestyle-rpc/pull/114))
* Updates build and Releases 0.7.0 ([#115](https://github.com/frees-io/freestyle-rpc/pull/115))

## 01/04/2018 - Version 0.6.1

Release changes:

* Docs - Empty.type Request/Response ([#105](https://github.com/frees-io/freestyle-rpc/pull/105))
* Upgrade to Freestyle 0.5.1 ([#107](https://github.com/frees-io/freestyle-rpc/pull/107))


## 12/21/2017 - Version 0.6.0

Release changes:

* Use Effect instance instead of Comonad#extract ([#103](https://github.com/frees-io/freestyle-rpc/pull/103))
* Compiled docs in frees-rpc repo ([#104](https://github.com/frees-io/freestyle-rpc/pull/104))


## 12/19/2017 - Version 0.5.2

Release changes:

* Excludes Guava from frees-async-guava ([#102](https://github.com/frees-io/freestyle-rpc/pull/102))


## 12/19/2017 - Version 0.5.1

Release changes:

* Supports inner imports within @service macro. ([#101](https://github.com/frees-io/freestyle-rpc/pull/101))


## 12/18/2017 - Version 0.5.0

Release changes:

* Upgrades to Freestyle 0.5.0 ([#99](https://github.com/frees-io/freestyle-rpc/pull/99))
* Adds additional SuppressWarnings built-in warts ([#100](https://github.com/frees-io/freestyle-rpc/pull/100))


## 12/18/2017 - Version 0.4.2

Release changes:

* Reduces boilerplate when creating client instances ([#97](https://github.com/frees-io/freestyle-rpc/pull/97))
* Reduces Boilerplate in Server creation ([#98](https://github.com/frees-io/freestyle-rpc/pull/98))


## 12/05/2017 - Version 0.4.1

Release changes:

* Server Endpoints and Effect Monad ([#95](https://github.com/frees-io/freestyle-rpc/pull/95))


## 12/01/2017 - Version 0.4.0

Release changes:

* Replace @free with @tagless, and drop the requirement of an annotation ([#92](https://github.com/frees-io/freestyle-rpc/pull/92))
* Upgrades frees-rpc to Freestyle 0.4.6 ([#94](https://github.com/frees-io/freestyle-rpc/pull/94))


## 11/23/2017 - Version 0.3.4

Release changes:

* Adds monix.eval.Task Comonad Implicit Evidence ([#89](https://github.com/frees-io/freestyle-rpc/pull/89))


## 11/22/2017 - Version 0.3.3

Release changes:

* Case class Empty is valid for Avro as well ([#87](https://github.com/frees-io/freestyle-rpc/pull/87))
* Fixes missing FQFN ([#88](https://github.com/frees-io/freestyle-rpc/pull/88))


## 11/17/2017 - Version 0.3.2

Release changes:

* Suppress wart warnings ([#85](https://github.com/frees-io/freestyle-rpc/pull/85))


## 11/16/2017 - Version 0.3.1

Release changes:

* Removes global imports ([#84](https://github.com/frees-io/freestyle-rpc/pull/84))


## 11/14/2017 - Version 0.3.0

Release changes:

* Support for Avro Serialization ([#78](https://github.com/frees-io/freestyle-rpc/pull/78))
* Async Implicits provided by frees-rpc Implicits ([#80](https://github.com/frees-io/freestyle-rpc/pull/80))
* Releases 0.3.0 ([#82](https://github.com/frees-io/freestyle-rpc/pull/82))


## 11/06/2017 - Version 0.2.0

Release changes:

* Upgrades to gRPC 1.7.0 ([#74](https://github.com/frees-io/freestyle-rpc/pull/74))
* Provides Empty Message ([#75](https://github.com/frees-io/freestyle-rpc/pull/75))
* Updates macros to avoid deprecation warnings ([#76](https://github.com/frees-io/freestyle-rpc/pull/76))
* Releases 0.2.0 ([#77](https://github.com/frees-io/freestyle-rpc/pull/77))


## 10/30/2017 - Version 0.1.2

Release changes:

* Provides an evidence where #67 shows up ([#68](https://github.com/frees-io/freestyle-rpc/pull/68))
* Groups async implicits into AsyncInstances trait ([#71](https://github.com/frees-io/freestyle-rpc/pull/71))


## 10/24/2017 - Version 0.1.1

Release changes:

* Removes Scalajs badge ([#62](https://github.com/frees-io/freestyle-rpc/pull/62))
* Upgrades to the latest version of sbt-freestyle ([#64](https://github.com/frees-io/freestyle-rpc/pull/64))


## 10/20/2017 - Version 0.1.0

Release changes:

* Test Coverage for some client definitions ([#57](https://github.com/frees-io/freestyle-rpc/pull/57))
* Test Coverage for client defs (Second Round) ([#58](https://github.com/frees-io/freestyle-rpc/pull/58))
* Test Coverage Server Definitions ([#60](https://github.com/frees-io/freestyle-rpc/pull/60))
* Releases 0.1.0 ([#61](https://github.com/frees-io/freestyle-rpc/pull/61))


## 10/17/2017 - Version 0.0.8

Release changes:

* Freestyle 0.4.0 Upgrade ([#56](https://github.com/frees-io/freestyle-rpc/pull/56))


## 10/10/2017 - Version 0.0.7

Release changes:

* Feature/common code in isolated artifact ([#55](https://github.com/frees-io/freestyle-rpc/pull/55))


## 10/09/2017 - Version 0.0.6

Release changes:

* Upgrades to sbt 1.0.1 and Scala 2.12.3 ([#48](https://github.com/frees-io/freestyle-rpc/pull/48))
* Brings sbt-frees-protogen as a separate Artifact ([#49](https://github.com/frees-io/freestyle-rpc/pull/49))
* Adds warning about generated proto files ([#50](https://github.com/frees-io/freestyle-rpc/pull/50))
* Fixes Travis Builds ([#52](https://github.com/frees-io/freestyle-rpc/pull/52))
* Fixes RPC build and Publishing Issues ([#53](https://github.com/frees-io/freestyle-rpc/pull/53))
* Removes protogen ([#54](https://github.com/frees-io/freestyle-rpc/pull/54))


## 10/09/2017 - Version 0.0.5

Release changes:

* Upgrades to sbt 1.0.1 and Scala 2.12.3 ([#48](https://github.com/frees-io/freestyle-rpc/pull/48))
* Brings sbt-frees-protogen as a separate Artifact ([#49](https://github.com/frees-io/freestyle-rpc/pull/49))
* Adds warning about generated proto files ([#50](https://github.com/frees-io/freestyle-rpc/pull/50))
* Fixes Travis Builds ([#52](https://github.com/frees-io/freestyle-rpc/pull/52))
* Fixes RPC build and Publishing Issues ([#53](https://github.com/frees-io/freestyle-rpc/pull/53))


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