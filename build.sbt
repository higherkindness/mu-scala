val scala213 = "2.13.8"
val scala3   = "3.1.2"

ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"
ThisBuild / scalaVersion       := scala213

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

// we expand this to (2.13, 3) on specific projects where we can cross-build
ThisBuild / crossScalaVersions := Seq(scala213)

publish / skip := true

addCommandAlias(
  "ci-test",
  "scalafmtCheckAll; scalafmtSbtCheck; missinglinkCheck; mdoc; +compile; +rpc-service/test; +tests/test; +haskell-integration-tests/test"
)
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll; publishMicrosite")
addCommandAlias("ci-publish", "github; ci-release")

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

////////////////
//// COMMON ////
////////////////

lazy val `rpc-service` = project
  .in(file("modules/service"))
  .settings(moduleName := "mu-rpc-service")
  .settings(rpcServiceSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

lazy val fs2 = project
  .in(file("modules/fs2"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-fs2")
  .settings(fs2Settings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(`rpc-service`)
  .dependsOn(server)
  .settings(moduleName := "mu-config")
  .settings(configSettings)

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "mu-rpc-testing")
  .settings(testingSettings)

////////////////
//// CLIENT ////
////////////////

lazy val `client-netty` = project
  .in(file("modules/client/netty"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-netty")
  .settings(clientNettySettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

lazy val `client-okhttp` = project
  .in(file("modules/client/okhttp"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-okhttp")
  .settings(clientOkHttpSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

lazy val `client-cache` = project
  .in(file("modules/client/cache"))
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)

///////////////////
//// NETTY SSL ////
///////////////////

lazy val `netty-ssl` = project
  .in(file("modules/netty-ssl"))
  .settings(moduleName := "mu-rpc-netty-ssl")
  .settings(nettySslSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

////////////////
//// SERVER ////
////////////////

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-server")
  .settings(serverSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

/////////////////////
//// HEALTHCHECK ////
/////////////////////

lazy val `health-check` = project
  .in(file("modules/health-check"))
  .enablePlugins(SrcGenPlugin)
  .dependsOn(`rpc-service`, fs2)
  .settings(healthCheckSettings)
  .settings(moduleName := "mu-rpc-health-check")

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val prometheus = project
  .in(file("modules/metrics/prometheus"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-prometheus")
  .settings(prometheusMetricsSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val dropwizard = project
  .in(file("modules/metrics/dropwizard"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-dropwizard")
  .settings(dropwizardMetricsSettings)
  .settings(crossScalaVersions := Seq(scala213, scala3))

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val `benchmarks-vnext` = project
  .in(file("benchmarks/vnext"))
  .dependsOn(server)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-core"  % V.log4cats,
      "org.typelevel" %% "log4cats-slf4j" % V.log4cats
    )
  )
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vnext")
  .settings(benchmarksSettings)
  .settings(publish / skip := true)
  .enablePlugins(JmhPlugin)

///////////////
//// TESTS ////
///////////////

lazy val tests = project
  .in(file("modules/tests"))
  .dependsOn(coreModulesDeps: _*)
  .settings(moduleName := "mu-rpc-tests")
  .settings(publish / skip := true)
  .settings(testSettings)

//////////////////////////////////////
//// MU-HASKELL INTEGRATION TESTS ////
//////////////////////////////////////

lazy val `haskell-integration-tests` = project
  .in(file("modules/haskell-integration-tests"))
  .settings(publish / skip := true)
  .settings(haskellIntegrationTestSettings)
  .dependsOn(server, `client-netty`, fs2)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val coreModules: Seq[ProjectReference] = Seq(
  `rpc-service`,
  fs2,
  `client-netty`,
  `client-okhttp`,
  `client-cache`,
  server,
  config,
  dropwizard,
  prometheus,
  testing,
  `netty-ssl`,
  `health-check`,
  `benchmarks-vnext` // TODO benchmarks can be removed from this list
)

lazy val coreModulesDeps: Seq[ClasspathDependency] =
  coreModules.map(ClasspathDependency(_, None))

lazy val microsite = project
  .dependsOn(coreModulesDeps: _*)
  .settings(docsSettings)
  .settings(micrositeSettings)
  .settings(publish / skip := true)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)

lazy val documentation = project
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)
  .enablePlugins(MdocPlugin)
