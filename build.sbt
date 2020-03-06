import sbtorgpolicies.model.scalac

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)

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
  .settings(internalSettings)

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

lazy val netty = project
  .in(file("modules/channel/netty"))
  .dependsOn(channel % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-netty")
  .settings(clientNettySettings)

lazy val okhttp = project
  .in(file("modules/channel/okhttp"))
  .dependsOn(channel % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-okhttp")
  .settings(clientOkHttpSettings)

lazy val ssl = project
  .in(file("modules/channel/netty-ssl"))
  .dependsOn(server % "test->test")
  .dependsOn(`netty` % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-netty-ssl")
  .settings(nettySslSettings)

////////////////
//// CLIENT ////
////////////////

lazy val `client-cache` = project
  .in(file("modules/client-cache"))
  .dependsOn(common % "test->test")
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)

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

////////////////
//// SRCGEN ////
////////////////

lazy val `srcgen-core` = project
  .in(file("modules/srcgen/core"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(channel % "test->test")
  .settings(moduleName := "mu-srcgen-core")
  .settings(srcGenSettings)
  .settings(compatSettings)

lazy val `srcgen-sbt` = project
  .in(file("modules/srcgen/plugin"))
  .dependsOn(`srcgen-core`)
  .settings(moduleName := "sbt-mu-srcgen")
  .settings(noCrossCompilationLastScala)
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "mu.rpc.srcgen")
  // See https://github.com/sbt/sbt/issues/3248
  .settings(
    publishLocal := publishLocal
      .dependsOn(
        common / publishLocal,
        `internal-core` / publishLocal,
        channel / publishLocal,
        server / publishLocal,
        `internal-fs2` / publishLocal,
        fs2 / publishLocal,
        `marshallers-jodatime` / publishLocal,
        `srcgen-core` / publishLocal
      )
      .value
  )
  .enablePlugins(SbtPlugin)

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val `benchmarks-vprev` = project
  .in(file("benchmarks/vprev"))
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-channel" % V.lastRelease,
      "io.higherkindness" %% "mu-rpc-server"  % V.lastRelease,
      "io.higherkindness" %% "mu-rpc-testing" % V.lastRelease
    )
  )
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vprev")
  .settings(crossSettings)
  .settings(noPublishSettings)
  .enablePlugins(JmhPlugin)

lazy val `benchmarks-vnext` = project
  .in(file("benchmarks/vnext"))
  .dependsOn(channel)
  .dependsOn(server)
  .dependsOn(testing)
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vnext")
  .settings(crossSettings)
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

///////////////////////////
//// DECIMAL MIGRATION ////
///////////////////////////

lazy val `legacy-avro-decimal-compat-protocol` = project
  .in(file("modules/legacy-avro-decimal/procotol"))
  .settings(moduleName := "legacy-avro-decimal-compat-protocol")
  .disablePlugins(scoverage.ScoverageSbtPlugin)

lazy val `legacy-avro-decimal-compat-model` = project
  .in(file("modules/legacy-avro-decimal/model"))
  .settings(moduleName := "legacy-avro-decimal-compat-model")

lazy val `legacy-avro-decimal-compat-encoders` = project
  .in(file("modules/legacy-avro-decimal/encoders"))
  .settings(moduleName := "legacy-avro-decimal-compat-encoders")
  .dependsOn(`legacy-avro-decimal-compat-model` % "provided")
  .dependsOn(`internal-core`)

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
  `client-cache`,
  netty,
  okhttp,
  server,
  config,
  dropwizard,
  prometheus,
  testing,
  ssl,
  `srcgen-core`,
  http,
  kafka,
  `marshallers-jodatime`,
  `legacy-avro-decimal-compat-model`,
  `legacy-avro-decimal-compat-protocol`,
  `legacy-avro-decimal-compat-encoders`,
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

lazy val `root-non-crossed-scala-versions` = project
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
  .settings(docsSettings: _*)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(noCrossCompilationLastScala)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)
