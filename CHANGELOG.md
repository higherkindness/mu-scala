# Changelog

## [v0.22.2](https://github.com/higherkindness/mu-scala/tree/v0.22.2) (2020-06-04)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.22.1...v0.22.2)

🚀 **Features**

- Prepare repository for next `.github` release and SBT build improvements [\#927](https://github.com/higherkindness/mu-scala/pull/927) ([juanpedromoreno](https://github.com/juanpedromoreno))

📘 **Documentation**

- Add new compendium plugin page [\#926](https://github.com/higherkindness/mu-scala/pull/926) ([mrtmmr](https://github.com/mrtmmr))

📈 **Dependency updates**

- Update sbt-tpolecat to 0.1.12 [\#925](https://github.com/higherkindness/mu-scala/pull/925) ([scala-steward](https://github.com/scala-steward))
- Update sbt to 1.3.11 [\#922](https://github.com/higherkindness/mu-scala/pull/922) ([scala-steward](https://github.com/scala-steward))
- Update simpleclient to 0.9.0 [\#919](https://github.com/higherkindness/mu-scala/pull/919) ([scala-steward](https://github.com/scala-steward))

**Merged pull requests:**

- Make the OkHttp and Netty tests actually use OkHttp/Netty [\#928](https://github.com/higherkindness/mu-scala/pull/928) ([cb372](https://github.com/cb372))
- Update base image for integration tests Docker images [\#921](https://github.com/higherkindness/mu-scala/pull/921) ([cb372](https://github.com/cb372))
- Upgrade integration tests to Mu-Haskell v0.3 [\#920](https://github.com/higherkindness/mu-scala/pull/920) ([cb372](https://github.com/cb372))

## [v0.22.1](https://github.com/higherkindness/mu-scala/tree/v0.22.1) (2020-05-11)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.22.0...v0.22.1)

📈 **Dependency updates**

- Update scalatest to 3.1.2 [\#917](https://github.com/higherkindness/mu-scala/pull/917) ([scala-steward](https://github.com/scala-steward))

**Merged pull requests:**

- Bumps pbdirect [\#915](https://github.com/higherkindness/mu-scala/pull/915) ([fedefernandez](https://github.com/fedefernandez))
- Update module names in documentation [\#914](https://github.com/higherkindness/mu-scala/pull/914) ([cb372](https://github.com/cb372))
- Replace use of Stream.eval\(...\).flatten with Stream.force [\#913](https://github.com/higherkindness/mu-scala/pull/913) ([cb372](https://github.com/cb372))

## [v0.22.0](https://github.com/higherkindness/mu-scala/tree/v0.22.0) (2020-05-05)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.21.3...v0.22.0)

⚠️ **Breaking changes**

- Remove annotations [\#908](https://github.com/higherkindness/mu-scala/pull/908) ([cb372](https://github.com/cb372))
- Sbt Modules Reorganization [\#900](https://github.com/higherkindness/mu-scala/pull/900) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Rename client modules [\#895](https://github.com/higherkindness/mu-scala/pull/895) ([cb372](https://github.com/cb372))
- Remove the legacy-avro-decimal-compat modules [\#891](https://github.com/higherkindness/mu-scala/pull/891) ([cb372](https://github.com/cb372))
- Wrap streaming responses in effect [\#871](https://github.com/higherkindness/mu-scala/pull/871) ([cb372](https://github.com/cb372))

🚀 **Features**

- Remove dependency on Guava [\#183](https://github.com/higherkindness/mu-scala/issues/183)
- Remove F.never from Resource initializer [\#883](https://github.com/higherkindness/mu-scala/pull/883) ([cb372](https://github.com/cb372))
- Remove sbt-scripted plugin [\#873](https://github.com/higherkindness/mu-scala/pull/873) ([cb372](https://github.com/cb372))
- Distributed tracing with Natchez [\#866](https://github.com/higherkindness/mu-scala/pull/866) ([cb372](https://github.com/cb372))
- Avro Mu-Haskell integration tests [\#861](https://github.com/higherkindness/mu-scala/pull/861) ([cb372](https://github.com/cb372))
- Removes plugin from mu-scala repo [\#860](https://github.com/higherkindness/mu-scala/pull/860) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Haskell integration tests [\#856](https://github.com/higherkindness/mu-scala/pull/856) ([cb372](https://github.com/cb372))
- Support compression on server side with FS2 streaming [\#847](https://github.com/higherkindness/mu-scala/pull/847) ([cb372](https://github.com/cb372))

📘 **Documentation**

- Add a note about the default transport layer in gRPC servers [\#885](https://github.com/higherkindness/mu-scala/pull/885) ([cb372](https://github.com/cb372))

🐛 **Bug Fixes**

- Fix example link in the readme [\#869](https://github.com/higherkindness/mu-scala/pull/869) ([BenFradet](https://github.com/BenFradet))
- Fix a few warnings [\#843](https://github.com/higherkindness/mu-scala/pull/843) ([cb372](https://github.com/cb372))

📈 **Dependency updates**

- Update metrics-core to 4.1.7 [\#903](https://github.com/higherkindness/mu-scala/pull/903) ([scala-steward](https://github.com/scala-steward))
- Update enumeratum to 1.6.0 [\#902](https://github.com/higherkindness/mu-scala/pull/902) ([scala-steward](https://github.com/scala-steward))
- Update grpc to 1.29.0 [\#898](https://github.com/higherkindness/mu-scala/pull/898) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update monix to 3.2.0 [\#892](https://github.com/higherkindness/mu-scala/pull/892) ([scala-steward](https://github.com/scala-steward))
- Update joda-time to 2.10.6 [\#890](https://github.com/higherkindness/mu-scala/pull/890) ([scala-steward](https://github.com/scala-steward))
- Update grpc-netty, grpc-okhttp, grpc-stub, ... to 1.28.1 [\#865](https://github.com/higherkindness/mu-scala/pull/865) ([scala-steward](https://github.com/scala-steward))

**Closed issues:**

- Major refactoring of the tests [\#877](https://github.com/higherkindness/mu-scala/issues/877)
- Refactor the macro [\#874](https://github.com/higherkindness/mu-scala/issues/874)
- About newer versions of gRPC and Performance [\#870](https://github.com/higherkindness/mu-scala/issues/870)
- Review the HTTP-related code in serviceImpl.scala [\#844](https://github.com/higherkindness/mu-scala/issues/844)
- Fix compiler warnings [\#827](https://github.com/higherkindness/mu-scala/issues/827)
- gRPC server does not shutdown after `sbt run` [\#826](https://github.com/higherkindness/mu-scala/issues/826)
- Audit and re-organise the sbt project structure [\#825](https://github.com/higherkindness/mu-scala/issues/825)

**Merged pull requests:**

- Fix invalid scaladoc [\#911](https://github.com/higherkindness/mu-scala/pull/911) ([cb372](https://github.com/cb372))
- Back to the new avro4s version [\#910](https://github.com/higherkindness/mu-scala/pull/910) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Enable -Xfatal-warnings for Scala 2.13 [\#909](https://github.com/higherkindness/mu-scala/pull/909) ([cb372](https://github.com/cb372))
- Update scalafmt-core to 2.5.1 [\#906](https://github.com/higherkindness/mu-scala/pull/906) ([BenFradet](https://github.com/BenFradet))
- Uses Temporary avro4s Release [\#905](https://github.com/higherkindness/mu-scala/pull/905) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update sbt-github to allow overriding organization [\#893](https://github.com/higherkindness/mu-scala/pull/893) ([alejandrohdezma](https://github.com/alejandrohdezma))
- Refactor the `@service` macro [\#887](https://github.com/higherkindness/mu-scala/pull/887) ([cb372](https://github.com/cb372))
- Using a "real" gRPC Server for Benchmarks [\#884](https://github.com/higherkindness/mu-scala/pull/884) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fix a bunch of compiler warnings [\#882](https://github.com/higherkindness/mu-scala/pull/882) ([cb372](https://github.com/cb372))
- Update scalacheck-toolbox-datetime to 0.3.4 [\#864](https://github.com/higherkindness/mu-scala/pull/864) ([scala-steward](https://github.com/scala-steward))
- Update sbt-org-policies to 0.13.3 [\#851](https://github.com/higherkindness/mu-scala/pull/851) ([scala-steward](https://github.com/scala-steward))

## [v0.21.3](https://github.com/higherkindness/mu-scala/tree/v0.21.3) (2020-03-11)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.21.2...v0.21.3)

**Merged pull requests:**

- Releases 0.21.3 [\#840](https://github.com/higherkindness/mu-scala/pull/840) ([juanpedromoreno](https://github.com/juanpedromoreno))
- muSrcGenIdlType Tweak [\#839](https://github.com/higherkindness/mu-scala/pull/839) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.21.2](https://github.com/higherkindness/mu-scala/tree/v0.21.2) (2020-03-11)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.21.1...v0.21.2)

**Merged pull requests:**

- Release 0.21.2 [\#838](https://github.com/higherkindness/mu-scala/pull/838) ([cb372](https://github.com/cb372))
- Fix "method too large" error [\#837](https://github.com/higherkindness/mu-scala/pull/837) ([cb372](https://github.com/cb372))
- Update scalacheck-toolbox-datetime to 0.3.3 [\#836](https://github.com/higherkindness/mu-scala/pull/836) ([scala-steward](https://github.com/scala-steward))

## [v0.21.1](https://github.com/higherkindness/mu-scala/tree/v0.21.1) (2020-03-10)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.21.0...v0.21.1)

**Merged pull requests:**

- Release 0.21.1 [\#834](https://github.com/higherkindness/mu-scala/pull/834) ([cb372](https://github.com/cb372))
- Fix release step to enable cross-publishing [\#833](https://github.com/higherkindness/mu-scala/pull/833) ([cb372](https://github.com/cb372))
- Update sbt-microsites to 1.1.3 [\#832](https://github.com/higherkindness/mu-scala/pull/832) ([scala-steward](https://github.com/scala-steward))

## [v0.21.0](https://github.com/higherkindness/mu-scala/tree/v0.21.0) (2020-03-10)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.20.1...v0.21.0)

🚀 **Features**

- Extract Examples from the current codebase [\#817](https://github.com/higherkindness/mu-scala/issues/817)
- mu AST skeuomorph printers for http clients [\#592](https://github.com/higherkindness/mu-scala/issues/592)
- HTTP Support from Protocols - Http2 Spike [\#205](https://github.com/higherkindness/mu-scala/issues/205)
- HTTP Support from Protocols [\#182](https://github.com/higherkindness/mu-scala/issues/182)

🐛 **Bug Fixes**

- Protoc import errors [\#613](https://github.com/higherkindness/mu-scala/issues/613)
- Enums in proto files fail. [\#611](https://github.com/higherkindness/mu-scala/issues/611)
- Default values sent by third party clients fails in rpc-server. [\#606](https://github.com/higherkindness/mu-scala/issues/606)
- Coproduct source geneation is broken [\#603](https://github.com/higherkindness/mu-scala/issues/603)

**Closed issues:**

- Cross compilation with scala 2.13 [\#787](https://github.com/higherkindness/mu-scala/issues/787)
- Prometheus server latency metrics no longer provide labels per service and method [\#635](https://github.com/higherkindness/mu-scala/issues/635)
- Protobuf oneof not decoding correctly [\#629](https://github.com/higherkindness/mu-scala/issues/629)
- Bump Avro4s [\#619](https://github.com/higherkindness/mu-scala/issues/619)
- Source generation fails with a package declaration [\#604](https://github.com/higherkindness/mu-scala/issues/604)
- OpenAPI support [\#549](https://github.com/higherkindness/mu-scala/issues/549)
- Clients should remain Scala.js compatible [\#21](https://github.com/higherkindness/mu-scala/issues/21)

**Merged pull requests:**

- Release 0.21.0 [\#831](https://github.com/higherkindness/mu-scala/pull/831) ([cb372](https://github.com/cb372))
- Add methods with Resource to GrpcServer and ManagedChannelInterpreter [\#830](https://github.com/higherkindness/mu-scala/pull/830) ([peterneyens](https://github.com/peterneyens))
- Update metrics-core to 4.1.5 [\#829](https://github.com/higherkindness/mu-scala/pull/829) ([scala-steward](https://github.com/scala-steward))
- Update avro4s-core to 3.0.9 [\#828](https://github.com/higherkindness/mu-scala/pull/828) ([scala-steward](https://github.com/scala-steward))
- Removes Examples from this Repository [\#824](https://github.com/higherkindness/mu-scala/pull/824) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Remove unnecessary publishLocal before running scripted tests [\#823](https://github.com/higherkindness/mu-scala/pull/823) ([cb372](https://github.com/cb372))
- Remove sbt-org-policies dependency syntax [\#822](https://github.com/higherkindness/mu-scala/pull/822) ([cb372](https://github.com/cb372))
- Update monocle-core to 2.0.4 [\#821](https://github.com/higherkindness/mu-scala/pull/821) ([scala-steward](https://github.com/scala-steward))
- Docs refresh [\#820](https://github.com/higherkindness/mu-scala/pull/820) ([cb372](https://github.com/cb372))
- Explicit scala version in Travis Script [\#819](https://github.com/higherkindness/mu-scala/pull/819) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fixes sbt Plugin Publish [\#818](https://github.com/higherkindness/mu-scala/pull/818) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Speed up the sbt plugin scripted tests [\#814](https://github.com/higherkindness/mu-scala/pull/814) ([cb372](https://github.com/cb372))
- Automatically register muSrcGen as a sourceGenerator [\#813](https://github.com/higherkindness/mu-scala/pull/813) ([cb372](https://github.com/cb372))
- Update cats-effect to 2.1.2 [\#812](https://github.com/higherkindness/mu-scala/pull/812) ([scala-steward](https://github.com/scala-steward))
- Update pureconfig to 0.12.3 [\#811](https://github.com/higherkindness/mu-scala/pull/811) ([scala-steward](https://github.com/scala-steward))
- Update metrics-core to 4.1.4 [\#810](https://github.com/higherkindness/mu-scala/pull/810) ([scala-steward](https://github.com/scala-steward))
- Fix nearly all the compiler warnings in production code and tests [\#809](https://github.com/higherkindness/mu-scala/pull/809) ([cb372](https://github.com/cb372))
- Cross compile code with Scala 2.13.x [\#807](https://github.com/higherkindness/mu-scala/pull/807) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update avro4s-core to 3.0.8 [\#806](https://github.com/higherkindness/mu-scala/pull/806) ([scala-steward](https://github.com/scala-steward))
- Update monocle-core to 2.0.2 [\#805](https://github.com/higherkindness/mu-scala/pull/805) ([scala-steward](https://github.com/scala-steward))
- Update skeuomorph to 0.0.22 [\#804](https://github.com/higherkindness/mu-scala/pull/804) ([scala-steward](https://github.com/scala-steward))
- Update sbt-org-policies to 0.13.1 [\#803](https://github.com/higherkindness/mu-scala/pull/803) ([scala-steward](https://github.com/scala-steward))
- Update metrics-core to 4.1.3 [\#802](https://github.com/higherkindness/mu-scala/pull/802) ([scala-steward](https://github.com/scala-steward))
- Update scalafmt-core to 2.4.2 [\#801](https://github.com/higherkindness/mu-scala/pull/801) ([scala-steward](https://github.com/scala-steward))
- Update pbdirect to 0.5.0 [\#800](https://github.com/higherkindness/mu-scala/pull/800) ([scala-steward](https://github.com/scala-steward))
- Update sbt-microsites to 1.1.2 [\#799](https://github.com/higherkindness/mu-scala/pull/799) ([scala-steward](https://github.com/scala-steward))
- Remove the `@message` annotation [\#798](https://github.com/higherkindness/mu-scala/pull/798) ([cb372](https://github.com/cb372))
- sbt-org-policies 0.13.0 [\#797](https://github.com/higherkindness/mu-scala/pull/797) ([BenFradet](https://github.com/BenFradet))
- Update scalatest to 3.1.1 [\#796](https://github.com/higherkindness/mu-scala/pull/796) ([scala-steward](https://github.com/scala-steward))
- Update scalafmt-core to 2.4.1 [\#792](https://github.com/higherkindness/mu-scala/pull/792) ([scala-steward](https://github.com/scala-steward))
- Mergify: configuration update [\#790](https://github.com/higherkindness/mu-scala/pull/790) ([angoglez](https://github.com/angoglez))
- Update circe-core, circe-generic, ... to 0.13.0 [\#784](https://github.com/higherkindness/mu-scala/pull/784) ([scala-steward](https://github.com/scala-steward))
- Update cats-effect to 2.1.1 [\#783](https://github.com/higherkindness/mu-scala/pull/783) ([scala-steward](https://github.com/scala-steward))
- Update scalacheck-toolbox-datetime to 0.3.2 [\#782](https://github.com/higherkindness/mu-scala/pull/782) ([scala-steward](https://github.com/scala-steward))
- Update sbt, scripted-plugin to 1.3.8 [\#779](https://github.com/higherkindness/mu-scala/pull/779) ([scala-steward](https://github.com/scala-steward))
- Update cats-effect to 2.1.0 [\#777](https://github.com/higherkindness/mu-scala/pull/777) ([scala-steward](https://github.com/scala-steward))
- Update fs2-core to 2.2.2 [\#776](https://github.com/higherkindness/mu-scala/pull/776) ([scala-steward](https://github.com/scala-steward))
- Introduce sum types for `idlType` and `serializationType` [\#773](https://github.com/higherkindness/mu-scala/pull/773) ([cb372](https://github.com/cb372))
- Adds a prometheus builder for full metrics [\#772](https://github.com/higherkindness/mu-scala/pull/772) ([fedefernandez](https://github.com/fedefernandez))
- Remove genOptions in sbt-mu-srcgen plugin [\#771](https://github.com/higherkindness/mu-scala/pull/771) ([naree](https://github.com/naree))
- Upgrade to avro4s 3.0.x [\#770](https://github.com/higherkindness/mu-scala/pull/770) ([cb372](https://github.com/cb372))
- Updates to scalatest 3.1.0 [\#769](https://github.com/higherkindness/mu-scala/pull/769) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Migrates from tut to mdoc [\#768](https://github.com/higherkindness/mu-scala/pull/768) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update the documentation for sbt-mu-srcgen key changes [\#765](https://github.com/higherkindness/mu-scala/pull/765) ([naree](https://github.com/naree))
- Rename srcgen sbt plugin keys [\#764](https://github.com/higherkindness/mu-scala/pull/764) ([naree](https://github.com/naree))
- Allows custom prometheus metrics collector [\#763](https://github.com/higherkindness/mu-scala/pull/763) ([fedefernandez](https://github.com/fedefernandez))
- Remove idl generation feature [\#761](https://github.com/higherkindness/mu-scala/pull/761) ([naree](https://github.com/naree))
- No Publish Healthcheck examples [\#760](https://github.com/higherkindness/mu-scala/pull/760) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Reapply the formatter after upgrading sbt-org-policies to 0.12.3 [\#759](https://github.com/higherkindness/mu-scala/pull/759) ([naree](https://github.com/naree))
- Set new Mu visual identity for the website [\#758](https://github.com/higherkindness/mu-scala/pull/758) ([calvellido](https://github.com/calvellido))
- Update simpleclient to 0.8.1 [\#757](https://github.com/higherkindness/mu-scala/pull/757) ([scala-steward](https://github.com/scala-steward))
- Overhaul the source generation docs [\#756](https://github.com/higherkindness/mu-scala/pull/756) ([cb372](https://github.com/cb372))
- Update sbt-org-policies to 0.12.3 [\#754](https://github.com/higherkindness/mu-scala/pull/754) ([scala-steward](https://github.com/scala-steward))
- Update fs2-core to 2.2.1 [\#753](https://github.com/higherkindness/mu-scala/pull/753) ([scala-steward](https://github.com/scala-steward))
- Upgrade sbt-org-policies and use bundled releases [\#751](https://github.com/higherkindness/mu-scala/pull/751) ([cb372](https://github.com/cb372))
- Bump version number and fix release step [\#750](https://github.com/higherkindness/mu-scala/pull/750) ([cb372](https://github.com/cb372))
- Rename idlgen to srcgen [\#749](https://github.com/higherkindness/mu-scala/pull/749) ([naree](https://github.com/naree))

## [v0.20.1](https://github.com/higherkindness/mu-scala/tree/v0.20.1) (2020-01-16)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.20.0...v0.20.1)

**Merged pull requests:**

- Release 0.20.1 [\#748](https://github.com/higherkindness/mu-scala/pull/748) ([cb372](https://github.com/cb372))
- Update sbt, scripted-plugin to 1.3.7 [\#747](https://github.com/higherkindness/mu-scala/pull/747) ([scala-steward](https://github.com/scala-steward))
- Scalameta codegen [\#746](https://github.com/higherkindness/mu-scala/pull/746) ([cb372](https://github.com/cb372))
- Update sbt-microsites to 1.1.0 [\#745](https://github.com/higherkindness/mu-scala/pull/745) ([scala-steward](https://github.com/scala-steward))
- Update pbdirect to 0.4.1 [\#744](https://github.com/higherkindness/mu-scala/pull/744) ([scala-steward](https://github.com/scala-steward))
- Update monocle-core to 2.0.1 [\#741](https://github.com/higherkindness/mu-scala/pull/741) ([scala-steward](https://github.com/scala-steward))
- Simplify Avro marshaller [\#740](https://github.com/higherkindness/mu-scala/pull/740) ([cb372](https://github.com/cb372))
- Add release notes for v0.20.0 [\#739](https://github.com/higherkindness/mu-scala/pull/739) ([cb372](https://github.com/cb372))

## [v0.20.0](https://github.com/higherkindness/mu-scala/tree/v0.20.0) (2020-01-03)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.19.1...v0.20.0)

🐛 **Bug Fixes**

- Extraneous imports [\#637](https://github.com/higherkindness/mu-scala/issues/637)

**Closed issues:**

- website does not exist [\#730](https://github.com/higherkindness/mu-scala/issues/730)
- Rename repository to mu-scala [\#582](https://github.com/higherkindness/mu-scala/issues/582)

**Merged pull requests:**

- Fix gitter channel badge [\#743](https://github.com/higherkindness/mu-scala/pull/743) ([fedefernandez](https://github.com/fedefernandez))
- Name the scripts in Travis [\#738](https://github.com/higherkindness/mu-scala/pull/738) ([cb372](https://github.com/cb372))
- Fixes project name [\#737](https://github.com/higherkindness/mu-scala/pull/737) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Release v0.20.0 [\#736](https://github.com/higherkindness/mu-scala/pull/736) ([cb372](https://github.com/cb372))
- Upgrade to pbdirect 0.4.0 and skeuomorph 0.0.19 [\#735](https://github.com/higherkindness/mu-scala/pull/735) ([cb372](https://github.com/cb372))
- Happy new year! [\#734](https://github.com/higherkindness/mu-scala/pull/734) ([cb372](https://github.com/cb372))
- Update enumeratum to 1.5.15 [\#733](https://github.com/higherkindness/mu-scala/pull/733) ([scala-steward](https://github.com/scala-steward))
- Update sbt to 1.3.6 [\#732](https://github.com/higherkindness/mu-scala/pull/732) ([scala-steward](https://github.com/scala-steward))
- Make the streaming implementation configurable [\#731](https://github.com/higherkindness/mu-scala/pull/731) ([cb372](https://github.com/cb372))
- Update pureconfig to 0.12.2 [\#729](https://github.com/higherkindness/mu-scala/pull/729) ([scala-steward](https://github.com/scala-steward))
- Update microsite using sbt-microsites 1.0.2 [\#728](https://github.com/higherkindness/mu-scala/pull/728) ([calvellido](https://github.com/calvellido))
- Update slf4j-nop to 1.7.30 [\#725](https://github.com/higherkindness/mu-scala/pull/725) ([scala-steward](https://github.com/scala-steward))
- Update embedded-kafka to 2.4.0 [\#724](https://github.com/higherkindness/mu-scala/pull/724) ([scala-steward](https://github.com/scala-steward))
- Update enumeratum to 1.5.14 [\#723](https://github.com/higherkindness/mu-scala/pull/723) ([scala-steward](https://github.com/scala-steward))
- Update scalacheck to 1.14.3 [\#722](https://github.com/higherkindness/mu-scala/pull/722) ([scala-steward](https://github.com/scala-steward))
- Update sbt, scripted-plugin to 1.3.5 [\#721](https://github.com/higherkindness/mu-scala/pull/721) ([scala-steward](https://github.com/scala-steward))
- Micro-optimisation: use IOUtils to copy InputStream to byte array [\#720](https://github.com/higherkindness/mu-scala/pull/720) ([cb372](https://github.com/cb372))
- Add a dependency on Enumeratum [\#719](https://github.com/higherkindness/mu-scala/pull/719) ([cb372](https://github.com/cb372))
- Bumps avrohugger to 1.0.0-RC22 [\#718](https://github.com/higherkindness/mu-scala/pull/718) ([fedefernandez](https://github.com/fedefernandez))
- Avoid adding unnecessary imports to generated source files [\#717](https://github.com/higherkindness/mu-scala/pull/717) ([cb372](https://github.com/cb372))
- Update metrics-core to 4.1.2 [\#715](https://github.com/higherkindness/mu-scala/pull/715) ([scala-steward](https://github.com/scala-steward))
- Various documentation fixes [\#714](https://github.com/higherkindness/mu-scala/pull/714) ([cb372](https://github.com/cb372))
- Update skeuomorph to 0.0.17 [\#713](https://github.com/higherkindness/mu-scala/pull/713) ([scala-steward](https://github.com/scala-steward))
- Update http4s-blaze-client, ... to 0.21.0-M6 [\#705](https://github.com/higherkindness/mu-scala/pull/705) ([scala-steward](https://github.com/scala-steward))
- Update sbt, scripted-plugin to 1.3.4 [\#703](https://github.com/higherkindness/mu-scala/pull/703) ([scala-steward](https://github.com/scala-steward))
- Update java-runtime to 0.6.0 [\#702](https://github.com/higherkindness/mu-scala/pull/702) ([scala-steward](https://github.com/scala-steward))
- Update sbt-scoverage to 1.6.1 [\#701](https://github.com/higherkindness/mu-scala/pull/701) ([scala-steward](https://github.com/scala-steward))
- Update java-runtime to 0.5.5 [\#700](https://github.com/higherkindness/mu-scala/pull/700) ([scala-steward](https://github.com/scala-steward))
- Update monix to 3.1.0 [\#699](https://github.com/higherkindness/mu-scala/pull/699) ([scala-steward](https://github.com/scala-steward))
- Update grpc-netty, grpc-okhttp, grpc-stub, ... to 1.24.2 [\#697](https://github.com/higherkindness/mu-scala/pull/697) ([scala-steward](https://github.com/scala-steward))
- Update fs2-core to 2.1.0 [\#695](https://github.com/higherkindness/mu-scala/pull/695) ([scala-steward](https://github.com/scala-steward))
- Update slf4j-nop to 1.7.29 [\#694](https://github.com/higherkindness/mu-scala/pull/694) ([scala-steward](https://github.com/scala-steward))
- Update java-runtime to 0.5.4 [\#693](https://github.com/higherkindness/mu-scala/pull/693) ([scala-steward](https://github.com/scala-steward))
- Update fs2-kafka to 0.20.2 [\#692](https://github.com/higherkindness/mu-scala/pull/692) ([scala-steward](https://github.com/scala-steward))
- Fixes docs and parallelize some travis jobs [\#691](https://github.com/higherkindness/mu-scala/pull/691) ([fedefernandez](https://github.com/fedefernandez))

## [v0.19.1](https://github.com/higherkindness/mu-scala/tree/v0.19.1) (2019-10-29)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.19.0...v0.19.1)

**Merged pull requests:**

- Releases 0.19.1 [\#690](https://github.com/higherkindness/mu-scala/pull/690) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update embedded-kafka to 2.3.1 [\#689](https://github.com/higherkindness/mu-scala/pull/689) ([scala-steward](https://github.com/scala-steward))
- Update joda-time to 2.10.5 [\#688](https://github.com/higherkindness/mu-scala/pull/688) ([scala-steward](https://github.com/scala-steward))
- Update simpleclient to 0.8.0 [\#687](https://github.com/higherkindness/mu-scala/pull/687) ([scala-steward](https://github.com/scala-steward))
- Update grpc-netty, grpc-okhttp, grpc-stub, ... to 1.24.1 [\#686](https://github.com/higherkindness/mu-scala/pull/686) ([scala-steward](https://github.com/scala-steward))
- Update circe-core, circe-generic, ... to 0.12.3 [\#685](https://github.com/higherkindness/mu-scala/pull/685) ([scala-steward](https://github.com/scala-steward))
- Update metrics-core to 4.1.1 [\#684](https://github.com/higherkindness/mu-scala/pull/684) ([scala-steward](https://github.com/scala-steward))
- Allow specifying custom serialization mechanism [\#683](https://github.com/higherkindness/mu-scala/pull/683) ([Chicker](https://github.com/Chicker))

## [v0.19.0](https://github.com/higherkindness/mu-scala/tree/v0.19.0) (2019-10-17)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.18.4...v0.19.0)

🐛 **Bug Fixes**

- Netty Prometheus Integration [\#674](https://github.com/higherkindness/mu-scala/issues/674)

**Closed issues:**

- Update mu-rpc-prometheus-server for 0.18.4 [\#673](https://github.com/higherkindness/mu-scala/issues/673)
- Support gRPC health check endpoint by default [\#626](https://github.com/higherkindness/mu-scala/issues/626)
- Best practices re: starting from Scala or starting from IDL [\#608](https://github.com/higherkindness/mu-scala/issues/608)
- Docs - Review quickstart modules [\#605](https://github.com/higherkindness/mu-scala/issues/605)
- Feature request: use idiomatic gRPC endpoint URLs [\#599](https://github.com/higherkindness/mu-scala/issues/599)

**Merged pull requests:**

- Update log4cats-core, log4cats-slf4j to 1.0.1 [\#681](https://github.com/higherkindness/mu-scala/pull/681) ([scala-steward](https://github.com/scala-steward))
- Releases 0.19.0 [\#680](https://github.com/higherkindness/mu-scala/pull/680) ([pepegar](https://github.com/pepegar))
- Update sbt-updates to 0.5.0 [\#679](https://github.com/higherkindness/mu-scala/pull/679) ([scala-steward](https://github.com/scala-steward))
- Update sbt, scripted-plugin to 1.3.3 [\#678](https://github.com/higherkindness/mu-scala/pull/678) ([scala-steward](https://github.com/scala-steward))
- Update circe-core, circe-generic, ... to 0.12.2 [\#676](https://github.com/higherkindness/mu-scala/pull/676) ([scala-steward](https://github.com/scala-steward))
- Update sbt-updates to 0.4.3 [\#675](https://github.com/higherkindness/mu-scala/pull/675) ([scala-steward](https://github.com/scala-steward))
- Update sbt-microsites to 0.9.7 [\#672](https://github.com/higherkindness/mu-scala/pull/672) ([scala-steward](https://github.com/scala-steward))
- Update pureconfig to 0.12.1 [\#671](https://github.com/higherkindness/mu-scala/pull/671) ([scala-steward](https://github.com/scala-steward))
- Update scalacheck to 1.14.2 [\#670](https://github.com/higherkindness/mu-scala/pull/670) ([scala-steward](https://github.com/scala-steward))
- Update grpc-netty, grpc-okhttp, grpc-stub, ... to 1.24.0 [\#669](https://github.com/higherkindness/mu-scala/pull/669) ([scala-steward](https://github.com/scala-steward))
- Update sbt-microsites to 0.9.6 [\#668](https://github.com/higherkindness/mu-scala/pull/668) ([scala-steward](https://github.com/scala-steward))
- Update simpleclient to 0.7.0 [\#667](https://github.com/higherkindness/mu-scala/pull/667) ([scala-steward](https://github.com/scala-steward))
- Update monocle-core to 2.0.0 [\#663](https://github.com/higherkindness/mu-scala/pull/663) ([scala-steward](https://github.com/scala-steward))
- Update monocle-core to 1.6.0 [\#661](https://github.com/higherkindness/mu-scala/pull/661) ([scala-steward](https://github.com/scala-steward))
- Update fs2-core to 2.0.1 [\#660](https://github.com/higherkindness/mu-scala/pull/660) ([scala-steward](https://github.com/scala-steward))
- Open api generation [\#659](https://github.com/higherkindness/mu-scala/pull/659) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades Project [\#642](https://github.com/higherkindness/mu-scala/pull/642) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades Sbt/Scala [\#641](https://github.com/higherkindness/mu-scala/pull/641) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Add avro source generation doc [\#640](https://github.com/higherkindness/mu-scala/pull/640) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Bump cats-effect to 2.0.0 [\#639](https://github.com/higherkindness/mu-scala/pull/639) ([BenFradet](https://github.com/BenFradet))
- HealthCheck endpoint and service [\#630](https://github.com/higherkindness/mu-scala/pull/630) ([mrtmmr](https://github.com/mrtmmr))
- Add import root to protobuf source generator [\#627](https://github.com/higherkindness/mu-scala/pull/627) ([bilki](https://github.com/bilki))
- Mmm/605 docs [\#625](https://github.com/higherkindness/mu-scala/pull/625) ([mrtmmr](https://github.com/mrtmmr))
- Fixes menu.yml [\#624](https://github.com/higherkindness/mu-scala/pull/624) ([fedefernandez](https://github.com/fedefernandez))

## [v0.18.4](https://github.com/higherkindness/mu-scala/tree/v0.18.4) (2019-07-08)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.18.3...v0.18.4)

**Closed issues:**

- Client cache should receive a ManagedChannel [\#563](https://github.com/higherkindness/mu-scala/issues/563)

**Merged pull requests:**

- Mu kafka management service [\#638](https://github.com/higherkindness/mu-scala/pull/638) ([BenFradet](https://github.com/BenFradet))
- Support for idiomatic endpoints in protobuf [\#623](https://github.com/higherkindness/mu-scala/pull/623) ([fedefernandez](https://github.com/fedefernandez))
- Allows generating idiomatic gRPC urls for avro [\#622](https://github.com/higherkindness/mu-scala/pull/622) ([fedefernandez](https://github.com/fedefernandez))
- Mu documentation changed - New section "Generate sources from IDL" [\#621](https://github.com/higherkindness/mu-scala/pull/621) ([mrtmmr](https://github.com/mrtmmr))
- Issue 563: Client cache should receive a ManagedChannel [\#617](https://github.com/higherkindness/mu-scala/pull/617) ([mrtmmr](https://github.com/mrtmmr))

## [v0.18.3](https://github.com/higherkindness/mu-scala/tree/v0.18.3) (2019-05-30)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.18.1...v0.18.3)

**Closed issues:**

- Optional fields [\#612](https://github.com/higherkindness/mu-scala/issues/612)

**Merged pull requests:**

- Fix compile errors with the generated source code [\#615](https://github.com/higherkindness/mu-scala/pull/615) ([noelmarkham](https://github.com/noelmarkham))
- Use updated Skeuomorph version for non-primitive protobuf fields [\#614](https://github.com/higherkindness/mu-scala/pull/614) ([noelmarkham](https://github.com/noelmarkham))

## [v0.18.1](https://github.com/higherkindness/mu-scala/tree/v0.18.1) (2019-05-07)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.18.0...v0.18.1)

**Closed issues:**

- Update Mu documentation [\#597](https://github.com/higherkindness/mu-scala/issues/597)

**Merged pull requests:**

- Fixes plugin release [\#610](https://github.com/higherkindness/mu-scala/pull/610) ([fedefernandez](https://github.com/fedefernandez))
- Fixes metric prefix [\#609](https://github.com/higherkindness/mu-scala/pull/609) ([fedefernandez](https://github.com/fedefernandez))
- Improves rpc metrics naming [\#607](https://github.com/higherkindness/mu-scala/pull/607) ([fedefernandez](https://github.com/fedefernandez))
- Improves how the params are received in the annotation [\#602](https://github.com/higherkindness/mu-scala/pull/602) ([fedefernandez](https://github.com/fedefernandez))
- Allows specifying the namespace and the capitalize params [\#601](https://github.com/higherkindness/mu-scala/pull/601) ([fedefernandez](https://github.com/fedefernandez))
- Update docs [\#600](https://github.com/higherkindness/mu-scala/pull/600) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Add seed sample [\#598](https://github.com/higherkindness/mu-scala/pull/598) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Bumps sbt-org-policies and sbt-jmh [\#596](https://github.com/higherkindness/mu-scala/pull/596) ([fedefernandez](https://github.com/fedefernandez))
- Fixes schema-evolution links [\#595](https://github.com/higherkindness/mu-scala/pull/595) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.18.0](https://github.com/higherkindness/mu-scala/tree/v0.18.0) (2019-04-11)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.17.2...v0.18.0)

🚀 **Features**

- Skipping binary dependencies for the HTTP capabilities [\#575](https://github.com/higherkindness/mu-scala/issues/575)
- Monix.Observable \<\> Fs2.Stream [\#574](https://github.com/higherkindness/mu-scala/issues/574)
- Reorganize Metrics modules [\#517](https://github.com/higherkindness/mu-scala/issues/517)
- Prometheus MetricOps implementation [\#513](https://github.com/higherkindness/mu-scala/issues/513)
- Code Generation from Proto definitions [\#379](https://github.com/higherkindness/mu-scala/issues/379)

🐛 **Bug Fixes**

- Prometheus Random Test failure [\#168](https://github.com/higherkindness/mu-scala/issues/168)

**Closed issues:**

- Propagates imports instead of cloning dependent messages [\#578](https://github.com/higherkindness/mu-scala/issues/578)
- Improve Dropwizard metrics support [\#504](https://github.com/higherkindness/mu-scala/issues/504)

**Merged pull requests:**

- Fixes travis snapshot release [\#594](https://github.com/higherkindness/mu-scala/pull/594) ([fedefernandez](https://github.com/fedefernandez))
- Fix decimal protocol [\#591](https://github.com/higherkindness/mu-scala/pull/591) ([fedefernandez](https://github.com/fedefernandez))
- Releases 0.18.0 [\#590](https://github.com/higherkindness/mu-scala/pull/590) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Minor fixes [\#588](https://github.com/higherkindness/mu-scala/pull/588) ([fedefernandez](https://github.com/fedefernandez))
- Removes the compatibility of Monix.Observable in the HTTP layer [\#587](https://github.com/higherkindness/mu-scala/pull/587) ([rafaparadela](https://github.com/rafaparadela))
- CommonRuntime - Converts tut:invisible by tut:silent [\#585](https://github.com/higherkindness/mu-scala/pull/585) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Remove the ExecutionContext implicit when not necessary [\#584](https://github.com/higherkindness/mu-scala/pull/584) ([fedefernandez](https://github.com/fedefernandez))
- Skipping binary dependencies when is not necessary [\#581](https://github.com/higherkindness/mu-scala/pull/581) ([rafaparadela](https://github.com/rafaparadela))
- Adapts latest changes in the code generation by Skeuomorph [\#579](https://github.com/higherkindness/mu-scala/pull/579) ([rafaparadela](https://github.com/rafaparadela))
- Skeuomorph integration and Proto Source Generation Support [\#577](https://github.com/higherkindness/mu-scala/pull/577) ([rafaparadela](https://github.com/rafaparadela))
- Removes deprecated Sbt Settings [\#573](https://github.com/higherkindness/mu-scala/pull/573) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Prometheus MetricsOps implementation [\#572](https://github.com/higherkindness/mu-scala/pull/572) ([jdesiloniz](https://github.com/jdesiloniz))
- Fixes typo in docs [\#571](https://github.com/higherkindness/mu-scala/pull/571) ([fedefernandez](https://github.com/fedefernandez))
- Enables benchmarks for the previous Mu version [\#570](https://github.com/higherkindness/mu-scala/pull/570) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Macro Fix - Uses the F param in macro [\#569](https://github.com/higherkindness/mu-scala/pull/569) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades Build [\#567](https://github.com/higherkindness/mu-scala/pull/567) ([juanpedromoreno](https://github.com/juanpedromoreno))
- RPC Server Start with Brackets [\#566](https://github.com/higherkindness/mu-scala/pull/566) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Fixes some package names and some refactors [\#565](https://github.com/higherkindness/mu-scala/pull/565) ([fedefernandez](https://github.com/fedefernandez))
- Update .travis.yml [\#562](https://github.com/higherkindness/mu-scala/pull/562) ([fedefernandez](https://github.com/fedefernandez))
- Moves the site publication to a new stage [\#561](https://github.com/higherkindness/mu-scala/pull/561) ([fedefernandez](https://github.com/fedefernandez))
- Update docs and reorganizes modules [\#560](https://github.com/higherkindness/mu-scala/pull/560) ([fedefernandez](https://github.com/fedefernandez))
- Sample http4s REST client/server with client macro derivation [\#552](https://github.com/higherkindness/mu-scala/pull/552) ([L-Lavigne](https://github.com/L-Lavigne))
- \[Docs\] Schema Evolution [\#481](https://github.com/higherkindness/mu-scala/pull/481) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.17.2](https://github.com/higherkindness/mu-scala/tree/v0.17.2) (2019-02-05)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.17.1...v0.17.2)

**Merged pull requests:**

- Releases 0.17.2 version [\#559](https://github.com/higherkindness/mu-scala/pull/559) ([fedefernandez](https://github.com/fedefernandez))
- Client cache resource and bumps resources [\#558](https://github.com/higherkindness/mu-scala/pull/558) ([fedefernandez](https://github.com/fedefernandez))
- Restores the sbt plugin release and fixes ci release [\#556](https://github.com/higherkindness/mu-scala/pull/556) ([fedefernandez](https://github.com/fedefernandez))

## [v0.17.1](https://github.com/higherkindness/mu-scala/tree/v0.17.1) (2019-01-31)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.17.0...v0.17.1)

🚀 **Features**

- Server Metrics implementation based on MetricOps  [\#516](https://github.com/higherkindness/mu-scala/issues/516)
- Client Metrics implementation based on MetricOps [\#515](https://github.com/higherkindness/mu-scala/issues/515)
- Update README Installation section [\#511](https://github.com/higherkindness/mu-scala/issues/511)

**Closed issues:**

- Update dependency com.github.julien-truffaut:monocle-core [\#541](https://github.com/higherkindness/mu-scala/issues/541)
- Dropwizard  MetricOps implementation [\#514](https://github.com/higherkindness/mu-scala/issues/514)

**Merged pull requests:**

- Fixes deployment [\#555](https://github.com/higherkindness/mu-scala/pull/555) ([fedefernandez](https://github.com/fedefernandez))
- Release 0.17.1 [\#554](https://github.com/higherkindness/mu-scala/pull/554) ([fedefernandez](https://github.com/fedefernandez))
- Downgrades avro4s [\#553](https://github.com/higherkindness/mu-scala/pull/553) ([fedefernandez](https://github.com/fedefernandez))
- Server metrics implementation based on MetricOps [\#550](https://github.com/higherkindness/mu-scala/pull/550) ([franciscodr](https://github.com/franciscodr))
- Fixes typo in the documentation of Dropwizard metrics [\#548](https://github.com/higherkindness/mu-scala/pull/548) ([franciscodr](https://github.com/franciscodr))
- Updates doc index [\#547](https://github.com/higherkindness/mu-scala/pull/547) ([fedefernandez](https://github.com/fedefernandez))
- 514 - Dropwizards implementation [\#546](https://github.com/higherkindness/mu-scala/pull/546) ([jdesiloniz](https://github.com/jdesiloniz))
- Adds a fake clock for testing the time meassure [\#545](https://github.com/higherkindness/mu-scala/pull/545) ([fedefernandez](https://github.com/fedefernandez))
- Removes duplicated doc from README [\#543](https://github.com/higherkindness/mu-scala/pull/543) ([fedefernandez](https://github.com/fedefernandez))
- Updates README [\#542](https://github.com/higherkindness/mu-scala/pull/542) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Channel Interceptor Metrics [\#522](https://github.com/higherkindness/mu-scala/pull/522) ([fedefernandez](https://github.com/fedefernandez))

## [v0.17.0](https://github.com/higherkindness/mu-scala/tree/v0.17.0) (2019-01-21)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.16.0...v0.17.0)

🚀 **Features**

- Define MetricOps algebra [\#512](https://github.com/higherkindness/mu-scala/issues/512)
- Splitting Mu: core + streaming modules [\#490](https://github.com/higherkindness/mu-scala/issues/490)
- Improve package and directory structure [\#480](https://github.com/higherkindness/mu-scala/issues/480)
- Code Coverage is broken [\#476](https://github.com/higherkindness/mu-scala/issues/476)
- RPC Clients and Referential Transparency [\#305](https://github.com/higherkindness/mu-scala/issues/305)
- Upgrade fs2-reactive-streams 0.6.0 [\#303](https://github.com/higherkindness/mu-scala/issues/303)
- Build Reorganization [\#518](https://github.com/higherkindness/mu-scala/pull/518) ([juanpedromoreno](https://github.com/juanpedromoreno))

🐛 **Bug Fixes**

- AVDL to Scala: When generating tagged BigDecimals as Optionals the shapeless.@@ import is missing [\#411](https://github.com/higherkindness/mu-scala/issues/411)
- fs2 streaming test random failure [\#376](https://github.com/higherkindness/mu-scala/issues/376)
- Arbitrary test failures [\#289](https://github.com/higherkindness/mu-scala/issues/289)
- Error since avro4s 1.9.0 [\#288](https://github.com/higherkindness/mu-scala/issues/288)
- Monix bidirectional-streaming random test failure [\#282](https://github.com/higherkindness/mu-scala/issues/282)
- FS2 bidirectional-streaming random test failure [\#164](https://github.com/higherkindness/mu-scala/issues/164)

**Closed issues:**

- Update dependency org.lyranthe.fs2-grpc:java-runtime [\#540](https://github.com/higherkindness/mu-scala/issues/540)
- Update dependency io.monix:monix [\#539](https://github.com/higherkindness/mu-scala/issues/539)
- Update dependency com.github.julien-truffaut:monocle-core [\#538](https://github.com/higherkindness/mu-scala/issues/538)
- Update dependency io.monix:monix [\#537](https://github.com/higherkindness/mu-scala/issues/537)
- Update dependency org.lyranthe.fs2-grpc:java-runtime [\#536](https://github.com/higherkindness/mu-scala/issues/536)
- Update dependency io.monix:monix [\#535](https://github.com/higherkindness/mu-scala/issues/535)
- Update dependency com.github.julien-truffaut:monocle-core [\#533](https://github.com/higherkindness/mu-scala/issues/533)
- Update dependency org.lyranthe.fs2-grpc:java-runtime [\#532](https://github.com/higherkindness/mu-scala/issues/532)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#531](https://github.com/higherkindness/mu-scala/issues/531)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#530](https://github.com/higherkindness/mu-scala/issues/530)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#529](https://github.com/higherkindness/mu-scala/issues/529)
- Update dependency io.monix:monix [\#528](https://github.com/higherkindness/mu-scala/issues/528)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#527](https://github.com/higherkindness/mu-scala/issues/527)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#526](https://github.com/higherkindness/mu-scala/issues/526)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#525](https://github.com/higherkindness/mu-scala/issues/525)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#524](https://github.com/higherkindness/mu-scala/issues/524)
- Review docs [\#502](https://github.com/higherkindness/mu-scala/issues/502)
- Unused import warning [\#491](https://github.com/higherkindness/mu-scala/issues/491)
- Metrics improvement - preliminary investigation [\#489](https://github.com/higherkindness/mu-scala/issues/489)
- Jekyll site [\#482](https://github.com/higherkindness/mu-scala/issues/482)
- Improvements to Mu docs for easier developer on boarding [\#474](https://github.com/higherkindness/mu-scala/issues/474)
- SPIKE: Investigate about fs2-grpc library [\#469](https://github.com/higherkindness/mu-scala/issues/469)
- BigDecimal tagged should be the default option in the SBT plugin [\#468](https://github.com/higherkindness/mu-scala/issues/468)
- Fix "Migration guide for decimal types" docs [\#467](https://github.com/higherkindness/mu-scala/issues/467)
- Update dependency io.circe:circe-core [\#461](https://github.com/higherkindness/mu-scala/issues/461)
- Update dependency io.netty:netty-tcnative-boringssl-static [\#460](https://github.com/higherkindness/mu-scala/issues/460)
- Update dependency io.grpc:grpc-okhttp [\#459](https://github.com/higherkindness/mu-scala/issues/459)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#458](https://github.com/higherkindness/mu-scala/issues/458)
- Update dependency io.grpc:grpc-netty [\#457](https://github.com/higherkindness/mu-scala/issues/457)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#456](https://github.com/higherkindness/mu-scala/issues/456)
- Update dependency io.grpc:grpc-netty [\#455](https://github.com/higherkindness/mu-scala/issues/455)
- Update dependency io.grpc:grpc-stub [\#454](https://github.com/higherkindness/mu-scala/issues/454)
- Update dependency io.grpc:grpc-netty:test [\#453](https://github.com/higherkindness/mu-scala/issues/453)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#452](https://github.com/higherkindness/mu-scala/issues/452)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#451](https://github.com/higherkindness/mu-scala/issues/451)
- Update dependency io.grpc:grpc-testing [\#450](https://github.com/higherkindness/mu-scala/issues/450)
- Update dependency io.monix:monix [\#449](https://github.com/higherkindness/mu-scala/issues/449)
- Update dependency co.fs2:fs2-core [\#448](https://github.com/higherkindness/mu-scala/issues/448)
- Update dependency io.grpc:grpc-core [\#447](https://github.com/higherkindness/mu-scala/issues/447)
- Update dependency io.circe:circe-parser [\#439](https://github.com/higherkindness/mu-scala/issues/439)
- Update dependency io.circe:circe-generic [\#438](https://github.com/higherkindness/mu-scala/issues/438)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#420](https://github.com/higherkindness/mu-scala/issues/420)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#419](https://github.com/higherkindness/mu-scala/issues/419)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#418](https://github.com/higherkindness/mu-scala/issues/418)

**Merged pull requests:**

- Disables dependencies plugin [\#534](https://github.com/higherkindness/mu-scala/pull/534) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fixes scripted tests [\#523](https://github.com/higherkindness/mu-scala/pull/523) ([fedefernandez](https://github.com/fedefernandez))
- Passes the method info to the MetricsOps defs [\#521](https://github.com/higherkindness/mu-scala/pull/521) ([fedefernandez](https://github.com/fedefernandez))
- Update gRPC and Others [\#520](https://github.com/higherkindness/mu-scala/pull/520) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Defines the MetricsOps algebra [\#519](https://github.com/higherkindness/mu-scala/pull/519) ([fedefernandez](https://github.com/fedefernandez))
- Bumps fs2-grpc [\#510](https://github.com/higherkindness/mu-scala/pull/510) ([fedefernandez](https://github.com/fedefernandez))
- `bindService` as an Effect [\#509](https://github.com/higherkindness/mu-scala/pull/509) ([fedefernandez](https://github.com/fedefernandez))
- Updates pbdirect [\#508](https://github.com/higherkindness/mu-scala/pull/508) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Updates license headers [\#507](https://github.com/higherkindness/mu-scala/pull/507) ([fedefernandez](https://github.com/fedefernandez))
- Fixes the avro decimal compat protocol namespace [\#506](https://github.com/higherkindness/mu-scala/pull/506) ([fedefernandez](https://github.com/fedefernandez))
- Split streaming modules [\#505](https://github.com/higherkindness/mu-scala/pull/505) ([fedefernandez](https://github.com/fedefernandez))
- Removes outdated references to freestyle [\#503](https://github.com/higherkindness/mu-scala/pull/503) ([fedefernandez](https://github.com/fedefernandez))
- Release "0.17.0" [\#501](https://github.com/higherkindness/mu-scala/pull/501) ([fedefernandez](https://github.com/fedefernandez))
- Fixes mu microsite base url [\#499](https://github.com/higherkindness/mu-scala/pull/499) ([juanpedromoreno](https://github.com/juanpedromoreno))
- New Mu Microsite [\#498](https://github.com/higherkindness/mu-scala/pull/498) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Ignores Metals files [\#497](https://github.com/higherkindness/mu-scala/pull/497) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Package renaming [\#495](https://github.com/higherkindness/mu-scala/pull/495) ([fedefernandez](https://github.com/fedefernandez))
- Improve managed channel interpreter [\#494](https://github.com/higherkindness/mu-scala/pull/494) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Makes the client extends the service [\#493](https://github.com/higherkindness/mu-scala/pull/493) ([fedefernandez](https://github.com/fedefernandez))
- Removes some warnings [\#488](https://github.com/higherkindness/mu-scala/pull/488) ([fedefernandez](https://github.com/fedefernandez))
- Bump up gRPC version [\#487](https://github.com/higherkindness/mu-scala/pull/487) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Downgrades sbt-org-policies and fixes codecov badge [\#486](https://github.com/higherkindness/mu-scala/pull/486) ([fedefernandez](https://github.com/fedefernandez))
- Defaults to ScalaBigDecimalTaggedGen in the idl-plugin [\#485](https://github.com/higherkindness/mu-scala/pull/485) ([fedefernandez](https://github.com/fedefernandez))
- Referential Transparency for RPC client [\#484](https://github.com/higherkindness/mu-scala/pull/484) ([franciscodr](https://github.com/franciscodr))
- Bumps avrohugger [\#483](https://github.com/higherkindness/mu-scala/pull/483) ([fedefernandez](https://github.com/fedefernandez))
- Code Coverage and Codecov support [\#478](https://github.com/higherkindness/mu-scala/pull/478) ([fedefernandez](https://github.com/fedefernandez))
- Uses fs2-grpc for the fs2 integration [\#477](https://github.com/higherkindness/mu-scala/pull/477) ([fedefernandez](https://github.com/fedefernandez))
- Docs update [\#475](https://github.com/higherkindness/mu-scala/pull/475) ([rlmark](https://github.com/rlmark))
- No need to liftIO in GrpcServer [\#473](https://github.com/higherkindness/mu-scala/pull/473) ([tbrown1979](https://github.com/tbrown1979))
- Upgrade Project [\#472](https://github.com/higherkindness/mu-scala/pull/472) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fixes decimal migration guide doc [\#470](https://github.com/higherkindness/mu-scala/pull/470) ([fedefernandez](https://github.com/fedefernandez))
- Dont block for server termination [\#465](https://github.com/higherkindness/mu-scala/pull/465) ([tbrown1979](https://github.com/tbrown1979))
- Update grpc deps to latest version [\#464](https://github.com/higherkindness/mu-scala/pull/464) ([juliano](https://github.com/juliano))
- Update circe deps to latest version [\#463](https://github.com/higherkindness/mu-scala/pull/463) ([juliano](https://github.com/juliano))
- Updates 0.16.0 Changelog File [\#462](https://github.com/higherkindness/mu-scala/pull/462) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.16.0](https://github.com/higherkindness/mu-scala/tree/v0.16.0) (2018-10-19)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.15.1...v0.16.0)

**Closed issues:**

- Update dependency io.circe:circe-core [\#437](https://github.com/higherkindness/mu-scala/issues/437)
- Update dependency io.netty:netty-tcnative-boringssl-static [\#436](https://github.com/higherkindness/mu-scala/issues/436)
- Update dependency io.grpc:grpc-netty:test [\#435](https://github.com/higherkindness/mu-scala/issues/435)
- Update dependency io.circe:circe-generic [\#434](https://github.com/higherkindness/mu-scala/issues/434)
- Update dependency io.grpc:grpc-okhttp [\#433](https://github.com/higherkindness/mu-scala/issues/433)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#432](https://github.com/higherkindness/mu-scala/issues/432)
- Update dependency io.grpc:grpc-netty [\#431](https://github.com/higherkindness/mu-scala/issues/431)
- Update dependency org.typelevel:cats-effect [\#430](https://github.com/higherkindness/mu-scala/issues/430)
- Update dependency io.grpc:grpc-netty:test [\#429](https://github.com/higherkindness/mu-scala/issues/429)
- Update dependency io.monix:monix [\#428](https://github.com/higherkindness/mu-scala/issues/428)
- Update dependency io.grpc:grpc-stub [\#427](https://github.com/higherkindness/mu-scala/issues/427)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#426](https://github.com/higherkindness/mu-scala/issues/426)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#425](https://github.com/higherkindness/mu-scala/issues/425)
- Update dependency io.grpc:grpc-testing [\#424](https://github.com/higherkindness/mu-scala/issues/424)
- Update dependency org.typelevel:cats-effect [\#423](https://github.com/higherkindness/mu-scala/issues/423)
- Update dependency co.fs2:fs2-core [\#422](https://github.com/higherkindness/mu-scala/issues/422)
- Update dependency org.typelevel:cats-effect:test [\#421](https://github.com/higherkindness/mu-scala/issues/421)
- Update dependency io.monix:monix [\#417](https://github.com/higherkindness/mu-scala/issues/417)
- Update dependency io.grpc:grpc-core [\#416](https://github.com/higherkindness/mu-scala/issues/416)
- Update dependency io.circe:circe-parser [\#404](https://github.com/higherkindness/mu-scala/issues/404)

**Merged pull requests:**

- Releases 0.16.0 [\#446](https://github.com/higherkindness/mu-scala/pull/446) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds New Publish Settings to Travis [\#445](https://github.com/higherkindness/mu-scala/pull/445) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Renames Project [\#444](https://github.com/higherkindness/mu-scala/pull/444) ([juanpedromoreno](https://github.com/juanpedromoreno))
- 0.15.1 - Update docs and files  [\#440](https://github.com/higherkindness/mu-scala/pull/440) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.15.1](https://github.com/higherkindness/mu-scala/tree/v0.15.1) (2018-10-07)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.15.0...v0.15.1)

🐛 **Bug Fixes**

- Avro4s decimal encoding [\#382](https://github.com/higherkindness/mu-scala/issues/382)

**Closed issues:**

- Freestyle-rpc Microsite [\#405](https://github.com/higherkindness/mu-scala/issues/405)
- Update dependency io.circe:circe-generic [\#403](https://github.com/higherkindness/mu-scala/issues/403)
- Update dependency io.circe:circe-core [\#402](https://github.com/higherkindness/mu-scala/issues/402)
- Update dependency io.netty:netty-tcnative-boringssl-static [\#401](https://github.com/higherkindness/mu-scala/issues/401)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#400](https://github.com/higherkindness/mu-scala/issues/400)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#399](https://github.com/higherkindness/mu-scala/issues/399)
- Update dependency io.circe:circe-generic [\#398](https://github.com/higherkindness/mu-scala/issues/398)
- Update dependency org.typelevel:cats-effect [\#397](https://github.com/higherkindness/mu-scala/issues/397)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#396](https://github.com/higherkindness/mu-scala/issues/396)
- Update dependency org.typelevel:cats-effect [\#395](https://github.com/higherkindness/mu-scala/issues/395)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#394](https://github.com/higherkindness/mu-scala/issues/394)
- Update dependency io.monix:monix [\#393](https://github.com/higherkindness/mu-scala/issues/393)
- Update dependency org.typelevel:cats-effect [\#392](https://github.com/higherkindness/mu-scala/issues/392)
- Update dependency org.typelevel:cats-effect:test [\#391](https://github.com/higherkindness/mu-scala/issues/391)
- Links to RPC examples is broken [\#380](https://github.com/higherkindness/mu-scala/issues/380)
- Update dependency io.netty:netty-tcnative-boringssl-static [\#367](https://github.com/higherkindness/mu-scala/issues/367)
- Update dependency io.grpc:grpc-netty:test [\#366](https://github.com/higherkindness/mu-scala/issues/366)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#365](https://github.com/higherkindness/mu-scala/issues/365)
- Update dependency io.grpc:grpc-netty [\#364](https://github.com/higherkindness/mu-scala/issues/364)
- Update dependency org.scalamacros:paradise:plugin-\>default\(compile\) [\#352](https://github.com/higherkindness/mu-scala/issues/352)
- Update dependency org.scalamacros:paradise:plugin-\>default\(compile\) [\#351](https://github.com/higherkindness/mu-scala/issues/351)
- Update dependency org.scalamacros:paradise:plugin-\>default\(compile\) [\#347](https://github.com/higherkindness/mu-scala/issues/347)
- Update dependency org.scalamacros:paradise:plugin-\>default\(compile\) [\#346](https://github.com/higherkindness/mu-scala/issues/346)
- Update dependency org.scalamacros:paradise:plugin-\>default\(compile\) [\#345](https://github.com/higherkindness/mu-scala/issues/345)
- Freestyle-RPC Benchmarks [\#228](https://github.com/higherkindness/mu-scala/issues/228)
- In-Process gRPC Server for Testing [\#227](https://github.com/higherkindness/mu-scala/issues/227)

**Merged pull requests:**

- Decoupling sbt-freestyle [\#414](https://github.com/higherkindness/mu-scala/pull/414) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Releases version 0.15.1 [\#413](https://github.com/higherkindness/mu-scala/pull/413) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Migration guide for decimals [\#412](https://github.com/higherkindness/mu-scala/pull/412) ([fedefernandez](https://github.com/fedefernandez))
- Fixes the tagged decimal code generation [\#410](https://github.com/higherkindness/mu-scala/pull/410) ([fedefernandez](https://github.com/fedefernandez))
- Adds support for BigDecimal tagged types [\#409](https://github.com/higherkindness/mu-scala/pull/409) ([fedefernandez](https://github.com/fedefernandez))
- Bumps Scala up to 2.12.7 [\#408](https://github.com/higherkindness/mu-scala/pull/408) ([juanpedromoreno](https://github.com/juanpedromoreno))
- First micro-site approach [\#407](https://github.com/higherkindness/mu-scala/pull/407) ([AntonioMateoGomez](https://github.com/AntonioMateoGomez))
- Running Benchmarks - Scripts [\#406](https://github.com/higherkindness/mu-scala/pull/406) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.15.0](https://github.com/higherkindness/mu-scala/tree/v0.15.0) (2018-09-26)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.14.1...v0.15.0)

🚀 **Features**

- Extract Non-Standard Type Marshallers to different compilation units [\#370](https://github.com/higherkindness/mu-scala/issues/370)
- Avro Marshallers for Date and Timestamp [\#269](https://github.com/higherkindness/mu-scala/issues/269)

**Closed issues:**

- Upgrade Avrohugger to RC12 for `Instant` type [\#381](https://github.com/higherkindness/mu-scala/issues/381)
- Custom Marshallers in Code Generation [\#372](https://github.com/higherkindness/mu-scala/issues/372)
- New Sbt code generation setting [\#371](https://github.com/higherkindness/mu-scala/issues/371)
- Update dependency io.netty:netty-tcnative-boringssl-static:test [\#363](https://github.com/higherkindness/mu-scala/issues/363)
- Update dependency io.grpc:grpc-netty [\#362](https://github.com/higherkindness/mu-scala/issues/362)
- Update dependency io.grpc:grpc-okhttp [\#361](https://github.com/higherkindness/mu-scala/issues/361)
- Update dependency io.grpc:grpc-netty:test [\#360](https://github.com/higherkindness/mu-scala/issues/360)
- Update dependency io.monix:monix [\#359](https://github.com/higherkindness/mu-scala/issues/359)
- Update dependency io.grpc:grpc-stub [\#358](https://github.com/higherkindness/mu-scala/issues/358)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#357](https://github.com/higherkindness/mu-scala/issues/357)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#356](https://github.com/higherkindness/mu-scala/issues/356)
- Update dependency com.github.julien-truffaut:monocle-core [\#355](https://github.com/higherkindness/mu-scala/issues/355)
- Update dependency io.prometheus:simpleclient [\#354](https://github.com/higherkindness/mu-scala/issues/354)
- Update dependency io.grpc:grpc-core [\#353](https://github.com/higherkindness/mu-scala/issues/353)
- Update dependency io.grpc:grpc-testing [\#350](https://github.com/higherkindness/mu-scala/issues/350)
- Update dependency joda-time:joda-time [\#349](https://github.com/higherkindness/mu-scala/issues/349)
- Update dependency com.47deg:scalacheck-toolbox-datetime:test [\#348](https://github.com/higherkindness/mu-scala/issues/348)
- Update dependency com.47deg:scalacheck-toolbox-datetime:test [\#344](https://github.com/higherkindness/mu-scala/issues/344)
- Update dependency io.monix:monix [\#343](https://github.com/higherkindness/mu-scala/issues/343)
- Support for more message field types [\#265](https://github.com/higherkindness/mu-scala/issues/265)

**Merged pull requests:**

- Releases 0.15.0 frees-rpc Version [\#390](https://github.com/higherkindness/mu-scala/pull/390) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Auto spin-up RPC server when running benchmark [\#389](https://github.com/higherkindness/mu-scala/pull/389) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds the avro and protobuffer serializers for java.time.Instant [\#388](https://github.com/higherkindness/mu-scala/pull/388) ([fedefernandez](https://github.com/fedefernandez))
- Upgrades Build Dependencies [\#387](https://github.com/higherkindness/mu-scala/pull/387) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Depending on Execution Context instead of Monix Scheduler [\#386](https://github.com/higherkindness/mu-scala/pull/386) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Allows Custom namespace for server/client metrics [\#385](https://github.com/higherkindness/mu-scala/pull/385) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Benchmarks - AvroWithSchema \(unary services\) [\#384](https://github.com/higherkindness/mu-scala/pull/384) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Benchmarks - Avro and Proto Unary Services [\#383](https://github.com/higherkindness/mu-scala/pull/383) ([juanpedromoreno](https://github.com/juanpedromoreno))
- document mandatory compiler plugin [\#378](https://github.com/higherkindness/mu-scala/pull/378) ([maaaikoool](https://github.com/maaaikoool))
- Updates docs with the custom codecs section [\#377](https://github.com/higherkindness/mu-scala/pull/377) ([fedefernandez](https://github.com/fedefernandez))
- Bumps io.grpc dependency [\#375](https://github.com/higherkindness/mu-scala/pull/375) ([fedefernandez](https://github.com/fedefernandez))
- Customize the codecs used in services through sbt [\#374](https://github.com/higherkindness/mu-scala/pull/374) ([fedefernandez](https://github.com/fedefernandez))
- BigDecimal and java.time encoders/decoders implicit instances are now optional [\#373](https://github.com/higherkindness/mu-scala/pull/373) ([fedefernandez](https://github.com/fedefernandez))
- Marshallers for serializing and deserializing joda.time dates [\#341](https://github.com/higherkindness/mu-scala/pull/341) ([AntonioMateoGomez](https://github.com/AntonioMateoGomez))

## [v0.14.1](https://github.com/higherkindness/mu-scala/tree/v0.14.1) (2018-07-17)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.14.0...v0.14.1)

🚀 **Features**

- Spike: Add some test cases for model evolutions [\#331](https://github.com/higherkindness/mu-scala/issues/331)

🐛 **Bug Fixes**

- Can't use Options or Lists of non-primitive types inside messages when using Protobuf serialization \(regression\) [\#285](https://github.com/higherkindness/mu-scala/issues/285)

**Merged pull requests:**

- Fixes options and lists serialization in proto [\#342](https://github.com/higherkindness/mu-scala/pull/342) ([fedefernandez](https://github.com/fedefernandez))
- Update 0.14.0 CHANGELOG [\#340](https://github.com/higherkindness/mu-scala/pull/340) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Avro Schema Backward and Forward Compatibility [\#334](https://github.com/higherkindness/mu-scala/pull/334) ([rafaparadela](https://github.com/rafaparadela))
- Utility method for encode joda time instances [\#276](https://github.com/higherkindness/mu-scala/pull/276) ([AntonioMateoGomez](https://github.com/AntonioMateoGomez))

## [v0.14.0](https://github.com/higherkindness/mu-scala/tree/v0.14.0) (2018-07-09)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.7...v0.14.0)

🚀 **Features**

- Marshallers as implicit params of the $client method [\#329](https://github.com/higherkindness/mu-scala/issues/329)
- Migrate IDL Generation [\#298](https://github.com/higherkindness/mu-scala/issues/298)
- Migrate Macro Annotations [\#291](https://github.com/higherkindness/mu-scala/issues/291)
- frees-rpc new Generation [\#290](https://github.com/higherkindness/mu-scala/issues/290)
- Marshallers Required Implicitly [\#278](https://github.com/higherkindness/mu-scala/issues/278)

**Closed issues:**

- Support for Compression in Code Generation [\#332](https://github.com/higherkindness/mu-scala/issues/332)
- Update dependency io.frees:frees-todolist-lib [\#323](https://github.com/higherkindness/mu-scala/issues/323)
- Update dependency io.frees:frees-todolist-lib [\#322](https://github.com/higherkindness/mu-scala/issues/322)
- Update dependency io.prometheus:simpleclient\_dropwizard [\#321](https://github.com/higherkindness/mu-scala/issues/321)
- Update dependency io.grpc:grpc-netty:test [\#320](https://github.com/higherkindness/mu-scala/issues/320)
- Update dependency io.grpc:grpc-okhttp [\#319](https://github.com/higherkindness/mu-scala/issues/319)
- Update dependency io.grpc:grpc-netty [\#318](https://github.com/higherkindness/mu-scala/issues/318)
- Update dependency io.grpc:grpc-stub [\#317](https://github.com/higherkindness/mu-scala/issues/317)
- Update dependency io.grpc:grpc-netty:test [\#316](https://github.com/higherkindness/mu-scala/issues/316)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#315](https://github.com/higherkindness/mu-scala/issues/315)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#314](https://github.com/higherkindness/mu-scala/issues/314)
- Update dependency io.prometheus:simpleclient [\#313](https://github.com/higherkindness/mu-scala/issues/313)
- Update dependency io.monix:monix [\#312](https://github.com/higherkindness/mu-scala/issues/312)
- Update dependency io.grpc:grpc-testing [\#311](https://github.com/higherkindness/mu-scala/issues/311)
- Update dependency io.monix:monix [\#310](https://github.com/higherkindness/mu-scala/issues/310)
- Update dependency io.grpc:grpc-core [\#309](https://github.com/higherkindness/mu-scala/issues/309)

**Merged pull requests:**

- Release frees-rpc 0.14.0 [\#339](https://github.com/higherkindness/mu-scala/pull/339) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Gzip Compression scripted tests [\#338](https://github.com/higherkindness/mu-scala/pull/338) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Add client cache sub project [\#337](https://github.com/higherkindness/mu-scala/pull/337) ([peterneyens](https://github.com/peterneyens))
- Upgrades monix to 3.0.0-RC1. [\#336](https://github.com/higherkindness/mu-scala/pull/336) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Refactor GrpcServer [\#333](https://github.com/higherkindness/mu-scala/pull/333) ([peterneyens](https://github.com/peterneyens))
- marshallers as implicit params [\#330](https://github.com/higherkindness/mu-scala/pull/330) ([pepegar](https://github.com/pepegar))
- Macro conversion [\#328](https://github.com/higherkindness/mu-scala/pull/328) ([pepegar](https://github.com/pepegar))

## [v0.13.7](https://github.com/higherkindness/mu-scala/tree/v0.13.7) (2018-06-07)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.6...v0.13.7)

🚀 **Features**

- Decouple from frees-logging [\#299](https://github.com/higherkindness/mu-scala/issues/299)
- Decouple from frees-core [\#295](https://github.com/higherkindness/mu-scala/issues/295)
- Decouple from frees-async-\* [\#293](https://github.com/higherkindness/mu-scala/issues/293)

**Merged pull requests:**

- Releases 0.13.7 [\#308](https://github.com/higherkindness/mu-scala/pull/308) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update Scala to 2.12.6 in TravisCI [\#306](https://github.com/higherkindness/mu-scala/pull/306) ([JesusMtnez](https://github.com/JesusMtnez))
- Upgrades Scala and Sbt versions [\#304](https://github.com/higherkindness/mu-scala/pull/304) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Decouples frees-async-cats-effect [\#302](https://github.com/higherkindness/mu-scala/pull/302) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.13.6](https://github.com/higherkindness/mu-scala/tree/v0.13.6) (2018-06-06)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.5...v0.13.6)

🚀 **Features**

- Decouple from frees-config [\#294](https://github.com/higherkindness/mu-scala/issues/294)
- Migrate @tagless annotations [\#292](https://github.com/higherkindness/mu-scala/issues/292)

**Merged pull requests:**

- Downgrade avro4s to 1.8.3 [\#301](https://github.com/higherkindness/mu-scala/pull/301) ([JesusMtnez](https://github.com/JesusMtnez))
- Decouple frees config [\#300](https://github.com/higherkindness/mu-scala/pull/300) ([pepegar](https://github.com/pepegar))
- decouple from frees-async [\#297](https://github.com/higherkindness/mu-scala/pull/297) ([pepegar](https://github.com/pepegar))
- replace all occurrences of @tagless annotation with the manual impl [\#296](https://github.com/higherkindness/mu-scala/pull/296) ([pepegar](https://github.com/pepegar))
- Re-ignoring failing tests, with reference to new issue [\#283](https://github.com/higherkindness/mu-scala/pull/283) ([L-Lavigne](https://github.com/L-Lavigne))
- Ignoring new bidirectional FS2 tests on Travis [\#281](https://github.com/higherkindness/mu-scala/pull/281) ([L-Lavigne](https://github.com/L-Lavigne))

## [v0.13.5](https://github.com/higherkindness/mu-scala/tree/v0.13.5) (2018-05-29)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.4...v0.13.5)

**Closed issues:**

- Move `withServerChannel` to `freestyle.rpc.testing.servers` [\#267](https://github.com/higherkindness/mu-scala/issues/267)
- Update dependency io.monix:monix [\#263](https://github.com/higherkindness/mu-scala/issues/263)
- Update dependency io.monix:monix [\#261](https://github.com/higherkindness/mu-scala/issues/261)
- Update dependency org.scalacheck:scalacheck:test [\#262](https://github.com/higherkindness/mu-scala/issues/262)
- Exception when calling toListL and similar functions on response streams without mapping them first [\#192](https://github.com/higherkindness/mu-scala/issues/192)
- Docs - Add client-side examples for streaming services [\#191](https://github.com/higherkindness/mu-scala/issues/191)

**Merged pull requests:**

- Update avro4s and avrohugger [\#280](https://github.com/higherkindness/mu-scala/pull/280) ([JesusMtnez](https://github.com/JesusMtnez))
- Rename srcJarNames to srcGenJarNames and fix deprecations [\#277](https://github.com/higherkindness/mu-scala/pull/277) ([L-Lavigne](https://github.com/L-Lavigne))
- Release 0.13.5 [\#275](https://github.com/higherkindness/mu-scala/pull/275) ([JesusMtnez](https://github.com/JesusMtnez))
- Bump avrohugger to 1.0.0-RC9 [\#274](https://github.com/higherkindness/mu-scala/pull/274) ([JesusMtnez](https://github.com/JesusMtnez))
- Support for serializing LocalDate and LocalDateTime values [\#273](https://github.com/higherkindness/mu-scala/pull/273) ([fedefernandez](https://github.com/fedefernandez))
- Adds a java time util for serializing dates [\#272](https://github.com/higherkindness/mu-scala/pull/272) ([fedefernandez](https://github.com/fedefernandez))
- BigDecimal serialization in protobuf and avro [\#271](https://github.com/higherkindness/mu-scala/pull/271) ([fedefernandez](https://github.com/fedefernandez))
- Exposing ServerChannel [\#268](https://github.com/higherkindness/mu-scala/pull/268) ([rafaparadela](https://github.com/rafaparadela))
- Fix \#192 \(crash with some server stream transformations\) [\#266](https://github.com/higherkindness/mu-scala/pull/266) ([L-Lavigne](https://github.com/L-Lavigne))
- noPublishSettings for RPC examples [\#264](https://github.com/higherkindness/mu-scala/pull/264) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.13.4](https://github.com/higherkindness/mu-scala/tree/v0.13.4) (2018-05-02)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.3...v0.13.4)

🚀 **Features**

- Removes the IDL core Dependency [\#254](https://github.com/higherkindness/mu-scala/pull/254) ([juanpedromoreno](https://github.com/juanpedromoreno))

**Closed issues:**

- Update dependency io.monix:monix [\#250](https://github.com/higherkindness/mu-scala/issues/250)

**Merged pull requests:**

- Releases 0.13.4 [\#260](https://github.com/higherkindness/mu-scala/pull/260) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Check if file exists before unzipping in idlgen plugin [\#259](https://github.com/higherkindness/mu-scala/pull/259) ([peterneyens](https://github.com/peterneyens))
- New example: TodoList application [\#256](https://github.com/higherkindness/mu-scala/pull/256) ([JesusMtnez](https://github.com/JesusMtnez))
- Added tests for RPC error handling, and a fix for StatusRuntimeException [\#252](https://github.com/higherkindness/mu-scala/pull/252) ([L-Lavigne](https://github.com/L-Lavigne))

## [v0.13.3](https://github.com/higherkindness/mu-scala/tree/v0.13.3) (2018-04-18)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.2...v0.13.3)

🚀 **Features**

- BigDecimal Serializers/Deserializers [\#239](https://github.com/higherkindness/mu-scala/issues/239)

**Closed issues:**

- Allows concatenate different directories to source files [\#240](https://github.com/higherkindness/mu-scala/issues/240)
- Update dependency io.monix:monix [\#235](https://github.com/higherkindness/mu-scala/issues/235)
- Move route guide example to RPC repository [\#222](https://github.com/higherkindness/mu-scala/issues/222)
- Create the client module with the protocol [\#221](https://github.com/higherkindness/mu-scala/issues/221)
- Create the server module with the protocol [\#220](https://github.com/higherkindness/mu-scala/issues/220)
- Add service sbt module to the example [\#219](https://github.com/higherkindness/mu-scala/issues/219)
- Add protocol sbt module to the example  [\#218](https://github.com/higherkindness/mu-scala/issues/218)

**Merged pull requests:**

- Fixes Server Helper. Releases 0.13.3. [\#249](https://github.com/higherkindness/mu-scala/pull/249) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Code Generation from IDL definitions placed in different sources [\#248](https://github.com/higherkindness/mu-scala/pull/248) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds support for Marshalling/Unmarshalling BigDecimals [\#244](https://github.com/higherkindness/mu-scala/pull/244) ([fedefernandez](https://github.com/fedefernandez))
- Allow a sequence of source generated directories [\#243](https://github.com/higherkindness/mu-scala/pull/243) ([AdrianRaFo](https://github.com/AdrianRaFo))
- fixing shutdown hook to run shutdown of server [\#238](https://github.com/higherkindness/mu-scala/pull/238) ([tyler-clark](https://github.com/tyler-clark))
- Add route guide example [\#236](https://github.com/higherkindness/mu-scala/pull/236) ([AdrianRaFo](https://github.com/AdrianRaFo))

## [v0.13.2](https://github.com/higherkindness/mu-scala/tree/v0.13.2) (2018-04-10)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.1...v0.13.2)

**Closed issues:**

- Update dependency io.monix:monix [\#226](https://github.com/higherkindness/mu-scala/issues/226)
- Create the examples structure [\#217](https://github.com/higherkindness/mu-scala/issues/217)

**Merged pull requests:**

- Releases Freestyle RPC 0.13.2 [\#234](https://github.com/higherkindness/mu-scala/pull/234) ([juanpedromoreno](https://github.com/juanpedromoreno))
- SBT - Adds AvroWithSchema Support [\#233](https://github.com/higherkindness/mu-scala/pull/233) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Spins up gRPC Servers forName [\#230](https://github.com/higherkindness/mu-scala/pull/230) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Avro Messages Serialized With Schemas [\#215](https://github.com/higherkindness/mu-scala/pull/215) ([tyler-clark](https://github.com/tyler-clark))

## [v0.13.1](https://github.com/higherkindness/mu-scala/tree/v0.13.1) (2018-04-08)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.13.0...v0.13.1)

**Closed issues:**

- Update dependency io.monix:monix [\#213](https://github.com/higherkindness/mu-scala/issues/213)

**Merged pull requests:**

- Support for packaged Avdl into jar dependencies [\#224](https://github.com/higherkindness/mu-scala/pull/224) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Enable conditionally disabling certain tests in Travis [\#216](https://github.com/higherkindness/mu-scala/pull/216) ([L-Lavigne](https://github.com/L-Lavigne))
- Releases the plugin first, then the core [\#214](https://github.com/higherkindness/mu-scala/pull/214) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.13.0](https://github.com/higherkindness/mu-scala/tree/v0.13.0) (2018-04-02)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.12.0...v0.13.0)

🚀 **Features**

- Support for generating Scala services from Avro IDL files [\#206](https://github.com/higherkindness/mu-scala/issues/206)

**Closed issues:**

- Update dependency io.circe:circe-generic [\#201](https://github.com/higherkindness/mu-scala/issues/201)
- Update dependency io.monix:monix [\#200](https://github.com/higherkindness/mu-scala/issues/200)
- Update dependency org.typelevel:cats-effect:test [\#199](https://github.com/higherkindness/mu-scala/issues/199)
- Update dependency org.scalameta:paradise:plugin-\>default\(compile\) [\#110](https://github.com/higherkindness/mu-scala/issues/110)

**Merged pull requests:**

- Release 0.13.0 [\#212](https://github.com/higherkindness/mu-scala/pull/212) ([L-Lavigne](https://github.com/L-Lavigne))
- Dependency updates [\#211](https://github.com/higherkindness/mu-scala/pull/211) ([L-Lavigne](https://github.com/L-Lavigne))
- Scala source generation from Avro IDL [\#210](https://github.com/higherkindness/mu-scala/pull/210) ([L-Lavigne](https://github.com/L-Lavigne))
- Ignore intermittently-failing tests on Travis [\#209](https://github.com/higherkindness/mu-scala/pull/209) ([L-Lavigne](https://github.com/L-Lavigne))
- Fixes in idlGen header, docs and tests [\#208](https://github.com/higherkindness/mu-scala/pull/208) ([L-Lavigne](https://github.com/L-Lavigne))
- Solves encoding issues in docs [\#207](https://github.com/higherkindness/mu-scala/pull/207) ([eperinan](https://github.com/eperinan))
- Fixes `idlgen-sbt` release Process in Travis [\#204](https://github.com/higherkindness/mu-scala/pull/204) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fixes title formatting in SSL/TLS [\#202](https://github.com/higherkindness/mu-scala/pull/202) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.12.0](https://github.com/higherkindness/mu-scala/tree/v0.12.0) (2018-03-19)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.11.1...v0.12.0)

🚀 **Features**

- Apache Avro IDL [\#181](https://github.com/higherkindness/mu-scala/issues/181)
- Docs - Split into different sections [\#176](https://github.com/higherkindness/mu-scala/issues/176)

**Closed issues:**

- Sbt Protogen Plugin Migration to the core [\#180](https://github.com/higherkindness/mu-scala/issues/180)
- Docs - Compression Support [\#179](https://github.com/higherkindness/mu-scala/issues/179)
- Docs - SSL/TLS Encryption Support \(Netty\) [\#178](https://github.com/higherkindness/mu-scala/issues/178)
- Docs - fs2 Streaming [\#177](https://github.com/higherkindness/mu-scala/issues/177)
- Update dependency io.monix:monix [\#175](https://github.com/higherkindness/mu-scala/issues/175)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#174](https://github.com/higherkindness/mu-scala/issues/174)
- Update dependency org.scalameta:scalameta [\#111](https://github.com/higherkindness/mu-scala/issues/111)

**Merged pull requests:**

- Releases frees-rpc 0.12.0 [\#198](https://github.com/higherkindness/mu-scala/pull/198) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Avro IDL Support [\#195](https://github.com/higherkindness/mu-scala/pull/195) ([L-Lavigne](https://github.com/L-Lavigne))
- Fixes Snapshot Publish [\#194](https://github.com/higherkindness/mu-scala/pull/194) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades to Freestyle 0.8.0 [\#193](https://github.com/higherkindness/mu-scala/pull/193) ([juanpedromoreno](https://github.com/juanpedromoreno))
- \[Docs\] Split into different sections [\#190](https://github.com/higherkindness/mu-scala/pull/190) ([eperinan](https://github.com/eperinan))
- Fixed stacktraces in tests caused by unclosed channels [\#189](https://github.com/higherkindness/mu-scala/pull/189) ([L-Lavigne](https://github.com/L-Lavigne))
- sbt build config refactoring, with dependency updates [\#188](https://github.com/higherkindness/mu-scala/pull/188) ([L-Lavigne](https://github.com/L-Lavigne))
- Project Upgrade [\#187](https://github.com/higherkindness/mu-scala/pull/187) ([juanpedromoreno](https://github.com/juanpedromoreno))
- IdlGen refactoring to prepare for eventual Avro support, with Proto generation style fixes [\#186](https://github.com/higherkindness/mu-scala/pull/186) ([L-Lavigne](https://github.com/L-Lavigne))
- Merge sbt-freestyle-protogen into freestyle-rpc codebase, and update @rpc processing to handle latest `freestyle-rpc syntax [\#184](https://github.com/higherkindness/mu-scala/pull/184) ([L-Lavigne](https://github.com/L-Lavigne))

## [v0.11.1](https://github.com/higherkindness/mu-scala/tree/v0.11.1) (2018-02-14)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.11.0...v0.11.1)

**Closed issues:**

- Update dependency io.monix:monix [\#171](https://github.com/higherkindness/mu-scala/issues/171)
- Update dependency com.sksamuel.avro4s:avro4s-core [\#170](https://github.com/higherkindness/mu-scala/issues/170)
- Update dependency com.github.zainab-ali:fs2-reactive-streams [\#169](https://github.com/higherkindness/mu-scala/issues/169)

**Merged pull requests:**

- Update fs2-reactive-streams and release 0.11.1 [\#173](https://github.com/higherkindness/mu-scala/pull/173) ([peterneyens](https://github.com/peterneyens))
- Readd support for companion objects [\#172](https://github.com/higherkindness/mu-scala/pull/172) ([peterneyens](https://github.com/peterneyens))

## [v0.11.0](https://github.com/higherkindness/mu-scala/tree/v0.11.0) (2018-02-13)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.10.0...v0.11.0)

**Closed issues:**

- Update dependency io.monix:monix [\#149](https://github.com/higherkindness/mu-scala/issues/149)
- Update dependency io.monix:monix [\#148](https://github.com/higherkindness/mu-scala/issues/148)
- Update dependency io.grpc:grpc-netty [\#136](https://github.com/higherkindness/mu-scala/issues/136)

**Merged pull requests:**

- Releases frees-rpc 0.11.0 [\#167](https://github.com/higherkindness/mu-scala/pull/167) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update grpc to 1.9.1 [\#166](https://github.com/higherkindness/mu-scala/pull/166) ([peterneyens](https://github.com/peterneyens))
- Add non request statements to `Client` [\#165](https://github.com/higherkindness/mu-scala/pull/165) ([peterneyens](https://github.com/peterneyens))
- Build Upgrade [\#163](https://github.com/higherkindness/mu-scala/pull/163) ([juanpedromoreno](https://github.com/juanpedromoreno))
- SSL/TLS Encryption Support \(Netty\) [\#162](https://github.com/higherkindness/mu-scala/pull/162) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Allows adding compression at method level [\#161](https://github.com/higherkindness/mu-scala/pull/161) ([fedefernandez](https://github.com/fedefernandez))
- Update fs2-reactive-streams [\#160](https://github.com/higherkindness/mu-scala/pull/160) ([peterneyens](https://github.com/peterneyens))
- Refactor service macro [\#159](https://github.com/higherkindness/mu-scala/pull/159) ([peterneyens](https://github.com/peterneyens))
- Upgrades to fs2-reactive-streams 0.4.0 [\#158](https://github.com/higherkindness/mu-scala/pull/158) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Change implicit StreamObserver conversions to syntax [\#157](https://github.com/higherkindness/mu-scala/pull/157) ([peterneyens](https://github.com/peterneyens))
- Upgrades fs2-reactive-streams lib [\#155](https://github.com/higherkindness/mu-scala/pull/155) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Updates build by using sbt-freestyle 0.13.16 [\#154](https://github.com/higherkindness/mu-scala/pull/154) ([juanpedromoreno](https://github.com/juanpedromoreno))
- fs2.Stream Support [\#152](https://github.com/higherkindness/mu-scala/pull/152) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Now the service requires an Effect instead of AsyncContext and `Task ~\> M` [\#150](https://github.com/higherkindness/mu-scala/pull/150) ([fedefernandez](https://github.com/fedefernandez))

## [v0.10.0](https://github.com/higherkindness/mu-scala/tree/v0.10.0) (2018-01-18)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.9.0...v0.10.0)

**Closed issues:**

- Update dependency io.grpc:grpc-okhttp [\#137](https://github.com/higherkindness/mu-scala/issues/137)
- Update dependency io.grpc:grpc-netty [\#135](https://github.com/higherkindness/mu-scala/issues/135)
- Update dependency io.grpc:grpc-testing:test [\#134](https://github.com/higherkindness/mu-scala/issues/134)
- Update dependency io.grpc:grpc-testing:test [\#133](https://github.com/higherkindness/mu-scala/issues/133)
- Update dependency io.grpc:grpc-stub [\#132](https://github.com/higherkindness/mu-scala/issues/132)
- Update dependency io.grpc:grpc-core [\#131](https://github.com/higherkindness/mu-scala/issues/131)
- Update dependency io.monix:monix [\#130](https://github.com/higherkindness/mu-scala/issues/130)
- Random Test Failure [\#65](https://github.com/higherkindness/mu-scala/issues/65)

**Merged pull requests:**

- Fixes random test failure [\#147](https://github.com/higherkindness/mu-scala/pull/147) ([fedefernandez](https://github.com/fedefernandez))
- Releases \*frees-rpc\* 0.10.0 [\#146](https://github.com/higherkindness/mu-scala/pull/146) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Updates Docs regarding Metrics Reporting [\#145](https://github.com/higherkindness/mu-scala/pull/145) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Monadic Server Start/RPC Calls/Stop in Tests [\#144](https://github.com/higherkindness/mu-scala/pull/144) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds some GRPC testing utilities [\#143](https://github.com/higherkindness/mu-scala/pull/143) ([fedefernandez](https://github.com/fedefernandez))
- Adds \*frees-rpc-testing\* including \*grpc-testing\* dependency [\#142](https://github.com/higherkindness/mu-scala/pull/142) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds Dropwizard Metrics Support [\#141](https://github.com/higherkindness/mu-scala/pull/141) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Metrics DSL [\#140](https://github.com/higherkindness/mu-scala/pull/140) ([juanpedromoreno](https://github.com/juanpedromoreno))
- gRPC Client Metrics using Prometheus [\#139](https://github.com/higherkindness/mu-scala/pull/139) ([juanpedromoreno](https://github.com/juanpedromoreno))
- gRPC Services Metrics using Prometheus [\#138](https://github.com/higherkindness/mu-scala/pull/138) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.9.0](https://github.com/higherkindness/mu-scala/tree/v0.9.0) (2018-01-12)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.8.0...v0.9.0)

**Closed issues:**

- Update dependency io.grpc:grpc-core [\#127](https://github.com/higherkindness/mu-scala/issues/127)
- Update dependency io.grpc:grpc-netty [\#126](https://github.com/higherkindness/mu-scala/issues/126)
- Update dependency io.grpc:grpc-okhttp [\#125](https://github.com/higherkindness/mu-scala/issues/125)
- Update dependency io.monix:monix [\#124](https://github.com/higherkindness/mu-scala/issues/124)
- Update dependency io.frees:frees-core:test [\#123](https://github.com/higherkindness/mu-scala/issues/123)
- Update dependency io.monix:monix [\#122](https://github.com/higherkindness/mu-scala/issues/122)
- Update dependency io.grpc:grpc-testing:test [\#121](https://github.com/higherkindness/mu-scala/issues/121)
- Update dependency io.grpc:grpc-stub [\#120](https://github.com/higherkindness/mu-scala/issues/120)
- Update dependency io.grpc:grpc-core [\#119](https://github.com/higherkindness/mu-scala/issues/119)

**Merged pull requests:**

- Upgrades to Freestyle 0.6.1. Releases 0.9.0. [\#129](https://github.com/higherkindness/mu-scala/pull/129) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Mini refactoring of `@service` [\#128](https://github.com/higherkindness/mu-scala/pull/128) ([peterneyens](https://github.com/peterneyens))
- Mini cleanup after move to finally tagless [\#118](https://github.com/higherkindness/mu-scala/pull/118) ([peterneyens](https://github.com/peterneyens))

## [v0.8.0](https://github.com/higherkindness/mu-scala/tree/v0.8.0) (2018-01-11)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.7.0...v0.8.0)

**Merged pull requests:**

- frees-rpc Tagless-final Migration - Release 0.8.0 [\#117](https://github.com/higherkindness/mu-scala/pull/117) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds the job in Travis for the after CI SBT task [\#116](https://github.com/higherkindness/mu-scala/pull/116) ([fedefernandez](https://github.com/fedefernandez))

## [v0.7.0](https://github.com/higherkindness/mu-scala/tree/v0.7.0) (2018-01-10)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.6.1...v0.7.0)

**Closed issues:**

- Update to Cats 1.0.0 [\#106](https://github.com/higherkindness/mu-scala/issues/106)
- Update dependency org.spire-math:kind-projector:plugin-\>default\(compile\) [\#93](https://github.com/higherkindness/mu-scala/issues/93)

**Merged pull requests:**

- Updates build and Releases 0.7.0 [\#115](https://github.com/higherkindness/mu-scala/pull/115) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Moves non-server tests to the root [\#114](https://github.com/higherkindness/mu-scala/pull/114) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Splits core into Server and Client submodules [\#113](https://github.com/higherkindness/mu-scala/pull/113) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Organizes all sbt modules under modules folder [\#112](https://github.com/higherkindness/mu-scala/pull/112) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Splits core module in \[core, config\] [\#109](https://github.com/higherkindness/mu-scala/pull/109) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Update build [\#108](https://github.com/higherkindness/mu-scala/pull/108) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.6.1](https://github.com/higherkindness/mu-scala/tree/v0.6.1) (2018-01-04)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.6.0...v0.6.1)

**Closed issues:**

- RPCAsyncImplicits Comonad Instances Tests [\#90](https://github.com/higherkindness/mu-scala/issues/90)

**Merged pull requests:**

- Upgrade to Freestyle 0.5.1 [\#107](https://github.com/higherkindness/mu-scala/pull/107) ([AdrianRaFo](https://github.com/AdrianRaFo))
- Docs - Empty.type Request/Response [\#105](https://github.com/higherkindness/mu-scala/pull/105) ([eperinan](https://github.com/eperinan))

## [v0.6.0](https://github.com/higherkindness/mu-scala/tree/v0.6.0) (2017-12-21)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.5.2...v0.6.0)

**Merged pull requests:**

- Compiled docs in frees-rpc repo [\#104](https://github.com/higherkindness/mu-scala/pull/104) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Use Effect instance instead of Comonad\#extract [\#103](https://github.com/higherkindness/mu-scala/pull/103) ([peterneyens](https://github.com/peterneyens))

## [v0.5.2](https://github.com/higherkindness/mu-scala/tree/v0.5.2) (2017-12-19)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.5.1...v0.5.2)

**Merged pull requests:**

- Excludes Guava from frees-async-guava [\#102](https://github.com/higherkindness/mu-scala/pull/102) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.5.1](https://github.com/higherkindness/mu-scala/tree/v0.5.1) (2017-12-19)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.5.0...v0.5.1)

**Merged pull requests:**

- Supports inner imports within @service macro. [\#101](https://github.com/higherkindness/mu-scala/pull/101) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.5.0](https://github.com/higherkindness/mu-scala/tree/v0.5.0) (2017-12-18)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.4.2...v0.5.0)

**Merged pull requests:**

- Adds additional SuppressWarnings built-in warts [\#100](https://github.com/higherkindness/mu-scala/pull/100) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades to Freestyle 0.5.0 [\#99](https://github.com/higherkindness/mu-scala/pull/99) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.4.2](https://github.com/higherkindness/mu-scala/tree/v0.4.2) (2017-12-18)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.4.1...v0.4.2)

**Merged pull requests:**

- Reduces Boilerplate in Server creation [\#98](https://github.com/higherkindness/mu-scala/pull/98) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Reduces boilerplate when creating client instances [\#97](https://github.com/higherkindness/mu-scala/pull/97) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.4.1](https://github.com/higherkindness/mu-scala/tree/v0.4.1) (2017-12-05)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.4.0...v0.4.1)

**Merged pull requests:**

- Server Endpoints and Effect Monad [\#95](https://github.com/higherkindness/mu-scala/pull/95) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.4.0](https://github.com/higherkindness/mu-scala/tree/v0.4.0) (2017-12-01)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.3.4...v0.4.0)

**Merged pull requests:**

- Upgrades frees-rpc to Freestyle 0.4.6 [\#94](https://github.com/higherkindness/mu-scala/pull/94) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Replace @free with @tagless, and drop the requirement of an annotation [\#92](https://github.com/higherkindness/mu-scala/pull/92) ([tbrown1979](https://github.com/tbrown1979))

## [v0.3.4](https://github.com/higherkindness/mu-scala/tree/v0.3.4) (2017-11-23)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.3.3...v0.3.4)

**Merged pull requests:**

- Adds monix.eval.Task Comonad Implicit Evidence [\#89](https://github.com/higherkindness/mu-scala/pull/89) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.3.3](https://github.com/higherkindness/mu-scala/tree/v0.3.3) (2017-11-22)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.3.2...v0.3.3)

🐛 **Bug Fixes**

- Empty for Avro [\#79](https://github.com/higherkindness/mu-scala/issues/79)

**Closed issues:**

- Needed EmptyResponse valid for avro/proto and response/request [\#86](https://github.com/higherkindness/mu-scala/issues/86)

**Merged pull requests:**

- Fixes missing FQFN [\#88](https://github.com/higherkindness/mu-scala/pull/88) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Case class Empty is valid for Avro as well [\#87](https://github.com/higherkindness/mu-scala/pull/87) ([eperinan](https://github.com/eperinan))

## [v0.3.2](https://github.com/higherkindness/mu-scala/tree/v0.3.2) (2017-11-17)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.3.1...v0.3.2)

**Merged pull requests:**

- Suppress wart warnings [\#85](https://github.com/higherkindness/mu-scala/pull/85) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.3.1](https://github.com/higherkindness/mu-scala/tree/v0.3.1) (2017-11-16)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.3.0...v0.3.1)

**Closed issues:**

- Remove Global Imports to avoid collisions [\#83](https://github.com/higherkindness/mu-scala/issues/83)

**Merged pull requests:**

- Removes global imports [\#84](https://github.com/higherkindness/mu-scala/pull/84) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.3.0](https://github.com/higherkindness/mu-scala/tree/v0.3.0) (2017-11-14)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.2.0...v0.3.0)

**Merged pull requests:**

- Releases 0.3.0 [\#82](https://github.com/higherkindness/mu-scala/pull/82) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Async Implicits provided by frees-rpc Implicits [\#80](https://github.com/higherkindness/mu-scala/pull/80) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Support for Avro Serialization [\#78](https://github.com/higherkindness/mu-scala/pull/78) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.2.0](https://github.com/higherkindness/mu-scala/tree/v0.2.0) (2017-11-06)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.1.2...v0.2.0)

🐛 **Bug Fixes**

- @rpc Services should allow empty argument list [\#63](https://github.com/higherkindness/mu-scala/issues/63)

**Closed issues:**

- Upgrade to gRPC 1.7.0 [\#72](https://github.com/higherkindness/mu-scala/issues/72)
- Remove `method create in object MethodDescriptor is deprecated` warning [\#69](https://github.com/higherkindness/mu-scala/issues/69)
- @rpc Service with Boolean Types as Request [\#67](https://github.com/higherkindness/mu-scala/issues/67)
- Generated Proto Files pointing to frees-rpc docs [\#51](https://github.com/higherkindness/mu-scala/issues/51)

**Merged pull requests:**

- Releases 0.2.0 [\#77](https://github.com/higherkindness/mu-scala/pull/77) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Updates macros to avoid deprecation warnings [\#76](https://github.com/higherkindness/mu-scala/pull/76) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Provides Empty Message [\#75](https://github.com/higherkindness/mu-scala/pull/75) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades to gRPC 1.7.0 [\#74](https://github.com/higherkindness/mu-scala/pull/74) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.1.2](https://github.com/higherkindness/mu-scala/tree/v0.1.2) (2017-10-30)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.1.1...v0.1.2)

**Closed issues:**

- Better Implicits Management [\#70](https://github.com/higherkindness/mu-scala/issues/70)
- grpc-testing scoped to Test [\#66](https://github.com/higherkindness/mu-scala/issues/66)

**Merged pull requests:**

- Groups async implicits into AsyncInstances trait [\#71](https://github.com/higherkindness/mu-scala/pull/71) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Provides an evidence where \#67 shows up [\#68](https://github.com/higherkindness/mu-scala/pull/68) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.1.1](https://github.com/higherkindness/mu-scala/tree/v0.1.1) (2017-10-24)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.1.0...v0.1.1)

**Closed issues:**

- Remove ScalaJS Badge [\#59](https://github.com/higherkindness/mu-scala/issues/59)

**Merged pull requests:**

- Upgrades to the latest version of sbt-freestyle [\#64](https://github.com/higherkindness/mu-scala/pull/64) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Removes Scalajs badge [\#62](https://github.com/higherkindness/mu-scala/pull/62) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.1.0](https://github.com/higherkindness/mu-scala/tree/v0.1.0) (2017-10-20)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.8...v0.1.0)

**Closed issues:**

- Test Coverage for Protocol Format Definitions [\#26](https://github.com/higherkindness/mu-scala/issues/26)
- Generate .proto files as part of the compile phase [\#25](https://github.com/higherkindness/mu-scala/issues/25)
- Test Coverage for Client Definitions [\#24](https://github.com/higherkindness/mu-scala/issues/24)

**Merged pull requests:**

- Releases 0.1.0 [\#61](https://github.com/higherkindness/mu-scala/pull/61) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Test Coverage Server Definitions [\#60](https://github.com/higherkindness/mu-scala/pull/60) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Test Coverage for client defs \(Second Round\) [\#58](https://github.com/higherkindness/mu-scala/pull/58) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Test Coverage for some client definitions [\#57](https://github.com/higherkindness/mu-scala/pull/57) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.8](https://github.com/higherkindness/mu-scala/tree/v0.0.8) (2017-10-17)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.7...v0.0.8)

**Closed issues:**

- Add Disclaimer to Generated Proto Files [\#47](https://github.com/higherkindness/mu-scala/issues/47)

**Merged pull requests:**

- Freestyle 0.4.0 Upgrade [\#56](https://github.com/higherkindness/mu-scala/pull/56) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.7](https://github.com/higherkindness/mu-scala/tree/v0.0.7) (2017-10-10)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.6...v0.0.7)

**Merged pull requests:**

- Feature/common code in isolated artifact [\#55](https://github.com/higherkindness/mu-scala/pull/55) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.6](https://github.com/higherkindness/mu-scala/tree/v0.0.6) (2017-10-09)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.5...v0.0.6)

**Merged pull requests:**

- Removes protogen [\#54](https://github.com/higherkindness/mu-scala/pull/54) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.5](https://github.com/higherkindness/mu-scala/tree/v0.0.5) (2017-10-09)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.3...v0.0.5)

**Closed issues:**

- Full Example freestyle-rpc [\#34](https://github.com/higherkindness/mu-scala/issues/34)

**Merged pull requests:**

- Adds warning about generated proto files [\#50](https://github.com/higherkindness/mu-scala/pull/50) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Brings sbt-frees-protogen as a separate Artifact [\#49](https://github.com/higherkindness/mu-scala/pull/49) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Upgrades to sbt 1.0.1 and Scala 2.12.3 [\#48](https://github.com/higherkindness/mu-scala/pull/48) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.3](https://github.com/higherkindness/mu-scala/tree/v0.0.3) (2017-10-03)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.2...v0.0.3)

**Merged pull requests:**

- Fixes Client Streaming rpc server [\#46](https://github.com/higherkindness/mu-scala/pull/46) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Makes the ChannelBuilder build a public method [\#45](https://github.com/higherkindness/mu-scala/pull/45) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.2](https://github.com/higherkindness/mu-scala/tree/v0.0.2) (2017-09-08)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/v0.0.1...v0.0.2)

**Merged pull requests:**

- Adds LoggingM as a part of GrpcServerApp module [\#44](https://github.com/higherkindness/mu-scala/pull/44) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Fixes proto code generator for repeated types [\#43](https://github.com/higherkindness/mu-scala/pull/43) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Bug Fix  Proto Code Generator for Custom Types [\#42](https://github.com/higherkindness/mu-scala/pull/42) ([juanpedromoreno](https://github.com/juanpedromoreno))

## [v0.0.1](https://github.com/higherkindness/mu-scala/tree/v0.0.1) (2017-09-05)

[Full Changelog](https://github.com/higherkindness/mu-scala/compare/c347452b7100929c85a6200d792b527c2473c86b...v0.0.1)

**Closed issues:**

- Renames Artifacts to frees-rpc [\#39](https://github.com/higherkindness/mu-scala/issues/39)
- Upgrade to grpc 1.6.0 [\#38](https://github.com/higherkindness/mu-scala/issues/38)
- Publish pbdirect under com.47deg org [\#37](https://github.com/higherkindness/mu-scala/issues/37)
-  Clients correlated with Service definitions for server/client streaming services [\#30](https://github.com/higherkindness/mu-scala/issues/30)
- Protocol Definitions - Server Macro Definitions [\#29](https://github.com/higherkindness/mu-scala/issues/29)
- Protocol Definitions - Client Macro Definitions [\#28](https://github.com/higherkindness/mu-scala/issues/28)
- Upgrade to sbt-freestyle 0.1.0 [\#18](https://github.com/higherkindness/mu-scala/issues/18)
- RPC Channel Configuration [\#17](https://github.com/higherkindness/mu-scala/issues/17)
- 2. Server Definitions - Test Coverage [\#15](https://github.com/higherkindness/mu-scala/issues/15)
- 3. Client Definitions [\#11](https://github.com/higherkindness/mu-scala/issues/11)
- 2. Server Definitions [\#10](https://github.com/higherkindness/mu-scala/issues/10)
- 1. Define Protocol Format Definitions [\#9](https://github.com/higherkindness/mu-scala/issues/9)
- Mezzo code copyright [\#3](https://github.com/higherkindness/mu-scala/issues/3)
- Serialization POC [\#2](https://github.com/higherkindness/mu-scala/issues/2)
- RPC Endpoint POC [\#1](https://github.com/higherkindness/mu-scala/issues/1)

**Merged pull requests:**

- Upgrades gRPC. Releases frees-rpc 0.0.1. [\#41](https://github.com/higherkindness/mu-scala/pull/41) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Renaming to frees-rpc. Moves examples to its own repository [\#40](https://github.com/higherkindness/mu-scala/pull/40) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Completes the basic Example [\#36](https://github.com/higherkindness/mu-scala/pull/36) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Minor fix [\#35](https://github.com/higherkindness/mu-scala/pull/35) ([AdrianRaFo](https://github.com/AdrianRaFo))
- monix.reactive.Observable for Streaming Services API [\#33](https://github.com/higherkindness/mu-scala/pull/33) ([juanpedromoreno](https://github.com/juanpedromoreno))
- RPC Client macro definitions [\#32](https://github.com/higherkindness/mu-scala/pull/32) ([raulraja](https://github.com/raulraja))
- @service Macro [\#31](https://github.com/higherkindness/mu-scala/pull/31) ([raulraja](https://github.com/raulraja))
- Adds tests for some client handlers [\#27](https://github.com/higherkindness/mu-scala/pull/27) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds additional server definitions tests [\#23](https://github.com/higherkindness/mu-scala/pull/23) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Server Definitions - Test Coverage [\#22](https://github.com/higherkindness/mu-scala/pull/22) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Server/Channel Configuration [\#20](https://github.com/higherkindness/mu-scala/pull/20) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Migrates to sbt-freestyle 0.1.0 [\#19](https://github.com/higherkindness/mu-scala/pull/19) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Client Definitions based on free algebras - Unary Services  [\#16](https://github.com/higherkindness/mu-scala/pull/16) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Provides a Demo Extension [\#14](https://github.com/higherkindness/mu-scala/pull/14) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Provides grpc configuration DSL and GrpcServer algebras [\#13](https://github.com/higherkindness/mu-scala/pull/13) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Generate .proto files from Freestyle service protocols [\#12](https://github.com/higherkindness/mu-scala/pull/12) ([raulraja](https://github.com/raulraja))
- Divides demo projects in two different sbt modules [\#8](https://github.com/higherkindness/mu-scala/pull/8) ([juanpedromoreno](https://github.com/juanpedromoreno))
- grpc-gateway Demo [\#7](https://github.com/higherkindness/mu-scala/pull/7) ([juanpedromoreno](https://github.com/juanpedromoreno))
- gRPC extended Demos [\#6](https://github.com/higherkindness/mu-scala/pull/6) ([juanpedromoreno](https://github.com/juanpedromoreno))
- Adds a dummy grpc demo for testing purposes [\#5](https://github.com/higherkindness/mu-scala/pull/5) ([juanpedromoreno](https://github.com/juanpedromoreno))



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
