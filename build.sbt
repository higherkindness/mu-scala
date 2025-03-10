val scala213 = "2.13.16"
val scala3   = "3.6.4"

ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"
ThisBuild / scalaVersion       := scala3

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

publish / skip := true

addCommandAlias(
  "all-tests",
  "config/test; tests/test; tests3/test; avro-rpc-tests/test; avro-rpc-tests3/test; protobuf-rpc-tests/test; protobuf-rpc-tests3/test"
)

addCommandAlias(
  "ci-test",
  "scalafmtCheckAll; scalafmtSbtCheck; missinglinkCheck; mdoc; all-tests"
)
addCommandAlias(
  "ci-docs",
  "github; documentation3/mdoc; headerCreateAll; microsite3/publishMicrosite"
)
addCommandAlias("ci-publish", "github; ci-release")

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

////////////////
//// COMMON ////
////////////////

lazy val `rpc-service` = projectMatrix
  .in(file("modules/service"))
  .settings(moduleName := "mu-rpc-service")
  .settings(rpcServiceSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val fs2 = projectMatrix
  .in(file("modules/fs2"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-fs2")
  .settings(fs2Settings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val config = projectMatrix
  .in(file("modules/config"))
  .dependsOn(`rpc-service`, server)
  .settings(moduleName := "mu-config")
  .settings(configSettings)
  .jvmPlatform(scalaVersions = Seq(scala213))

lazy val testing = projectMatrix
  .in(file("modules/testing"))
  .settings(moduleName := "mu-rpc-testing")
  .settings(testingSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

////////////////
//// CLIENT ////
////////////////

lazy val `client-netty` = projectMatrix
  .in(file("modules/client/netty"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-netty")
  .settings(clientNettySettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val `client-okhttp` = projectMatrix
  .in(file("modules/client/okhttp"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-okhttp")
  .settings(clientOkHttpSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val `client-cache` = projectMatrix
  .in(file("modules/client/cache"))
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

///////////////////
//// NETTY SSL ////
///////////////////

lazy val `netty-ssl` = projectMatrix
  .in(file("modules/netty-ssl"))
  .settings(moduleName := "mu-rpc-netty-ssl")
  .settings(nettySslSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

////////////////
//// SERVER ////
////////////////

lazy val server = projectMatrix
  .in(file("modules/server"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-server")
  .settings(serverSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

/////////////////////
//// HEALTHCHECK ////
/////////////////////

lazy val `health-check` = projectMatrix
  .in(file("modules/health-check"))
  .enablePlugins(SrcGenPlugin)
  .dependsOn(`rpc-service`, fs2)
  .settings(healthCheckSettings)
  .settings(moduleName := "mu-rpc-health-check")
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val prometheus = projectMatrix
  .in(file("modules/metrics/prometheus"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-prometheus")
  .settings(prometheusMetricsSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val dropwizard = projectMatrix
  .in(file("modules/metrics/dropwizard"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-dropwizard")
  .settings(dropwizardMetricsSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val `benchmarks-vnext` = projectMatrix
  .in(file("benchmarks/vnext"))
  .dependsOn(server)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-core"  % V.log4cats,
      "org.typelevel" %% "log4cats-slf4j" % V.log4cats
    )
  )
  .settings(moduleName := "mu-benchmarks-vnext")
  .settings(benchmarksSettings)
  .settings(publish / skip := true)
  .jvmPlatform(scalaVersions = Seq(scala213))
  .enablePlugins(JmhPlugin)

///////////////
//// TESTS ////
///////////////

lazy val `test-utils` = projectMatrix
  .in(file("modules/tests/utils"))
  .dependsOn(crossBuiltModuleDeps: _*)
  .settings(moduleName := "mu-rpc-test-utils")
  .settings(publish / skip := true)
  .settings(testUtilsSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val tests = projectMatrix
  .in(file("modules/tests"))
  .dependsOn(crossBuiltModuleDeps: _*)
  .dependsOn(`test-utils`)
  .settings(moduleName := "mu-rpc-tests")
  .settings(publish / skip := true)
  .settings(testSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val `protobuf-rpc-tests` = projectMatrix
  .in(file("modules/tests/rpc/proto"))
  .enablePlugins(SrcGenPlugin)
  .dependsOn(crossBuiltModuleDeps: _*)
  .dependsOn(`test-utils`)
  .settings(moduleName := "mu-rpc-protobuf-tests")
  .settings(publish / skip := true)
  .settings(protobufRPCTestSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

lazy val `avro-rpc-tests` = projectMatrix
  .in(file("modules/tests/rpc/avro"))
  .enablePlugins(SrcGenPlugin)
  .dependsOn(crossBuiltModuleDeps: _*)
  .settings(moduleName := "mu-rpc-avro-tests")
  .settings(publish / skip := true)
  .settings(avroRPCTestSettings)
  .jvmPlatform(scalaVersions = Seq(scala3, scala213))

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val crossBuiltModules: Seq[sbt.internal.ProjectMatrix] = Seq(
  `rpc-service`,
  fs2,
  `client-netty`,
  `client-okhttp`,
  `client-cache`,
  server,
  dropwizard,
  prometheus,
  testing,
  `netty-ssl`,
  `health-check`
)

lazy val crossBuiltModuleDeps: Seq[
  sbt.internal.MatrixClasspathDep[sbt.internal.ProjectMatrixReference]
] =
  crossBuiltModules.map(m =>
    sbt.internal.ProjectMatrix.MatrixClasspathDependency(m, configuration = None)
  )

///////////////////
//// MICROSITE ////
///////////////////

lazy val `microsite-examples-protobuf` = projectMatrix
  .in(file("microsite/examples/proto"))
  .dependsOn(`rpc-service`, fs2)
  .enablePlugins(SrcGenPlugin)
  .settings(publish / skip := true)
  .settings(protobufSrcGenSettings)
  .jvmPlatform(scalaVersions = Seq(scala3))

lazy val `microsite-examples-avro` = projectMatrix
  .in(file("microsite/examples/avro"))
  .dependsOn(`rpc-service`)
  .enablePlugins(SrcGenPlugin)
  .settings(publish / skip := true)
  .settings(avroSrcGenSettings)
  .jvmPlatform(scalaVersions = Seq(scala3))

lazy val microsite = projectMatrix
  .dependsOn(crossBuiltModuleDeps: _*)
  .dependsOn(`microsite-examples-protobuf`, `microsite-examples-avro`)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)
  .settings(docsSettings)
  .settings(micrositeSettings)
  .settings(publish / skip := true)
  .settings(
    excludeDependencies ++= Seq(
      /*
       * Exclude _2.13 version of these libraries,
       because we also have the _3 version on the classpath.

       mdoc_3 -> mdoc-cli_3 -> scalameta_2.13 -> scalapb-runtime_2.13
       */
      ExclusionRule("com.thesamet.scalapb", "scalapb-runtime_2.13"),
      ExclusionRule("com.thesamet.scalapb", "lenses_2.13")
    )
  )
  .jvmPlatform(scalaVersions = Seq(scala3))

/////////////////////////////////
//// GENERATED DOCUMENTATION ////
/////////////////////////////////

lazy val documentation = projectMatrix
  .enablePlugins(MdocPlugin)
  .settings(
    mdocOut            := file("."),
    mdocExtraArguments := Seq("--no-link-hygiene")
  )
  .settings(publish / skip := true)
  .jvmPlatform(scalaVersions = Seq(scala3))
