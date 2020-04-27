ThisBuild / organization := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"

lazy val checkScalafmt         = "+scalafmtCheck; +scalafmtSbtCheck;"
lazy val checkBenchmarks       = "benchmarks-root/test;"
lazy val checkDocs             = "+docs/mdoc;"
lazy val checkIntegrationTests = "+haskell-integration-tests/test;"
lazy val checkTests            = "+coverage; +test; +coverageReport; +coverageAggregate;"

addCommandAlias(
  "ci-test",
  s"$checkScalafmt $checkBenchmarks $checkDocs $checkIntegrationTests $checkTests"
)
addCommandAlias("ci-docs", "project-docs/mdoc; docs/mdoc; headerCreateAll")
addCommandAlias("ci-microsite", "docs/publishMicrosite")

////////////////
//// COMMON ////
////////////////

lazy val common = project
  .in(file("modules/common"))
  .settings(moduleName := "mu-common")
  .settings(commonSettings)

lazy val `internal-core` = project
  .in(file("modules/internal"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-internal-core")
  .settings(internalCoreSettings)

lazy val `internal-monix` = project
  .in(file("modules/internal/monix"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-internal-monix")
  .settings(internalMonixSettings)

lazy val `internal-fs2` = project
  .in(file("modules/internal/fs2"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-internal-fs2")
  .settings(internalFs2Settings)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(common % "test->test")
  .dependsOn(channel % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-config")
  .settings(configSettings)

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "mu-rpc-testing")
  .settings(testingSettings)

///////////////////////////////////
//// TRANSPORT LAYER - CHANNEL ////
///////////////////////////////////

lazy val channel = project
  .in(file("modules/channel"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(`internal-fs2` % "test->test")
  .dependsOn(`internal-monix` % "test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-channel")
  .settings(clientCoreSettings)

lazy val monix = project
  .in(file("modules/streaming/monix"))
  .dependsOn(channel)
  .dependsOn(`health-check-unary`)
  .dependsOn(`internal-monix`)
  .settings(moduleName := "mu-rpc-monix")

lazy val fs2 = project
  .in(file("modules/streaming/fs2"))
  .dependsOn(channel)
  .dependsOn(`health-check-unary`)
  .dependsOn(`internal-fs2`)
  .settings(moduleName := "mu-rpc-fs2")

////////////////
//// CLIENT ////
////////////////

lazy val `client-netty` = project
  .in(file("modules/client/netty"))
  .dependsOn(channel % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-client-netty")
  .settings(clientNettySettings)

lazy val `client-okhttp` = project
  .in(file("modules/client/okhttp"))
  .dependsOn(channel % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-client-okhttp")
  .settings(clientOkHttpSettings)

lazy val `client-cache` = project
  .in(file("modules/client/cache"))
  .dependsOn(common % "test->test")
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)

///////////////////
//// NETTY SSL ////
///////////////////

lazy val `netty-ssl` = project
  .in(file("modules/netty-ssl"))
  .dependsOn(server % "test->test")
  .dependsOn(`client-netty` % "test->test")
  .settings(moduleName := "mu-rpc-netty-ssl")
  .settings(nettySslSettings)

////////////////
//// SERVER ////
////////////////

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(`internal-monix` % "test->test")
  .dependsOn(`internal-fs2` % "test->test")
  .dependsOn(channel % "test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-server")
  .settings(serverSettings)

/////////////////////
//// HEALTHCHECK ////
/////////////////////

lazy val `health-check-unary` = project
  .in(file("modules/health-check-unary"))
  .dependsOn(channel)
  .settings(healthCheckSettings)
  .settings(moduleName := "mu-rpc-health-check-unary")

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val prometheus = project
  .in(file("modules/metrics/prometheus"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-prometheus")
  .settings(prometheusMetricsSettings)

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val dropwizard = project
  .in(file("modules/metrics/dropwizard"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-dropwizard")
  .settings(dropwizardMetricsSettings)

///////////////////
//// HTTP/REST ////
///////////////////

lazy val http = project
  .in(file("modules/http"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-http")
  .settings(httpSettings)

///////////////
//// KAFKA ////
///////////////

lazy val kafka = project
  .in(file("modules/kafka"))
  .dependsOn(channel)
  .dependsOn(server % "test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-kafka")
  .settings(kafkaSettings)

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val `benchmarks-vprev` = project
  .in(file("benchmarks/vprev"))
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-channel" % V.lastRelease,
      "io.higherkindness" %% "mu-rpc-server"  % V.lastRelease
    )
  )
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vprev")
  .settings(benchmarksSettings)
  .settings(noPublishSettings)
  .enablePlugins(JmhPlugin)

lazy val `benchmarks-vnext` = project
  .in(file("benchmarks/vnext"))
  .dependsOn(channel)
  .dependsOn(server)
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vnext")
  .settings(benchmarksSettings)
  .settings(noPublishSettings)
  .enablePlugins(JmhPlugin)

/////////////////////
//// MARSHALLERS ////
/////////////////////

lazy val `marshallers-jodatime` = project
  .in(file("modules/marshallers/jodatime"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(channel % "compile->compile;test->test")
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-marshallers-jodatime")
  .settings(marshallersJodatimeSettings)

//////////////////////////////////////
//// MU-HASKELL INTEGRATION TESTS ////
//////////////////////////////////////

lazy val `haskell-integration-tests` = project
  .in(file("modules/haskell-integration-tests"))
  .settings(noPublishSettings)
  .settings(haskellIntegrationTestSettings)
  .dependsOn(server, `client-netty`, channel, fs2)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val coreModules: Seq[ProjectReference] = Seq(
  common,
  `internal-core`,
  `internal-monix`,
  `internal-fs2`,
  channel,
  monix,
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
  http,
  kafka,
  `marshallers-jodatime`,
  `health-check-unary`
)

lazy val nonCrossedScalaVersionModules: Seq[ProjectReference] = Seq(
  `benchmarks-vprev`,
  `benchmarks-vnext`
)

lazy val coreModulesDeps: Seq[ClasspathDependency] =
  coreModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "mu-scala")
  .settings(noPublishSettings)
  .aggregate(coreModules: _*)
  .dependsOn(coreModulesDeps: _*)

lazy val `benchmarks-root` = project
  .in(file("benchmarks"))
  .settings(name := "mu-scala-benchmarks")
  .settings(noPublishSettings)
  .settings(noCrossCompilationLastScala)
  .aggregate(nonCrossedScalaVersionModules: _*)
  .dependsOn(nonCrossedScalaVersionModules.map(ClasspathDependency(_, None)): _*)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(coreModulesDeps: _*)
  .settings(name := "mu-docs")
  .settings(docsSettings)
  .settings(micrositeSettings)
  .settings(noPublishSettings)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)

lazy val `project-docs` = (project in file(".docs"))
  .aggregate(coreModules: _*)
  .dependsOn(coreModulesDeps: _*)
  .settings(moduleName := "mu-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(noPublishSettings)
  .enablePlugins(MdocPlugin)
