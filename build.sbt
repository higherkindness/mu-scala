import sbtorgpolicies.model.scalac

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

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
  .dependsOn(`internal-monix`)
  .settings(moduleName := "mu-rpc-monix")

lazy val fs2 = project
  .in(file("modules/streaming/fs2"))
  .dependsOn(channel)
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
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)

////////////////
//// SERVER ////
////////////////

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(channel % "test->test")
  .dependsOn(monix % "test->test")
  .dependsOn(fs2 % "test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-server")
  .settings(serverSettings)

//////////////////////
//// INTERCEPTORS ////
//////////////////////

lazy val interceptors = project
  .in(file("modules/interceptors"))
  .settings(moduleName := "mu-rpc-interceptors")
  .settings(interceptorsSettings)

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val `prometheus-shared` = project
  .in(file("modules/prometheus/shared"))
  .dependsOn(interceptors % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-prometheus-shared")
  .settings(prometheusSettings)

lazy val `prometheus-server` = project
  .in(file("modules/prometheus/server"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-prometheus-server")

lazy val `prometheus-client` = project
  .in(file("modules/prometheus/client"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(channel % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "mu-rpc-prometheus-client")
  .settings(prometheusClientSettings)

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val `dropwizard-server` = project
  .in(file("modules/dropwizard/server"))
  .dependsOn(`prometheus-server` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "mu-rpc-dropwizard-server")
  .settings(dropwizardSettings)

lazy val `dropwizard-client` = project
  .in(file("modules/dropwizard/client"))
  .dependsOn(`prometheus-client` % "compile->compile;test->test")
  .dependsOn(channel % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "mu-rpc-dropwizard-client")
  .settings(dropwizardSettings)

lazy val `dropwizard` = project
  .in(file("modules/metrics/dropwizard"))
  .dependsOn(`internal-core`)
  .dependsOn(testing % "test->test")
  .settings(moduleName := "mu-rpc-dropwizard")
  .settings(dropwizardMetricsSettings)

////////////////
//// IDLGEN ////
////////////////

lazy val `idlgen-core` = project
  .in(file("modules/idlgen/core"))
  .dependsOn(`internal-core` % "compile->compile;test->test")
  .dependsOn(channel % "test->test")
  .settings(moduleName := "mu-idlgen-core")
  .settings(idlGenSettings)

lazy val `idlgen-sbt` = project
  .in(file("modules/idlgen/plugin"))
  .dependsOn(`idlgen-core`)
  .settings(moduleName := "sbt-mu-idlgen")
  .settings(crossScalaVersions := Seq(scalac.`2.12`))
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "mu.rpc.idlgen")
  .enablePlugins(SbtPlugin)

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val lastReleasedV = "0.15.1"

lazy val `benchmarks-vprev` = project
  .in(file("benchmarks/vprev"))
  // TODO: temporarily disabled until the project is migrated
//  .settings(
//    libraryDependencies ++= Seq(
//      "io.higherkindness" %% "mu-rpc-channel" % lastReleasedV,
//      "io.higherkindness" %% "mu-rpc-server"      % lastReleasedV,
//      "io.higherkindness" %% "mu-rpc-testing"     % lastReleasedV
//    )
//  )
  // TODO: remove dependsOn and uncomment the lines above
  .dependsOn(channel)
  .dependsOn(server)
  .dependsOn(testing)
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

//////////////////
//// EXAMPLES ////
//////////////////

////////////////////
//// ROUTEGUIDE ////
////////////////////

lazy val `example-routeguide-protocol` = project
  .in(file("modules/examples/routeguide/protocol"))
  .dependsOn(monix)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-routeguide-protocol")

lazy val `example-routeguide-runtime` = project
  .in(file("modules/examples/routeguide/runtime"))
  .settings(noPublishSettings)
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-rpc-example-routeguide-runtime")
  .settings(exampleRouteguideRuntimeSettings)

lazy val `example-routeguide-common` = project
  .in(file("modules/examples/routeguide/common"))
  .dependsOn(`example-routeguide-protocol`)
  .dependsOn(config)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-routeguide-common")
  .settings(exampleRouteguideCommonSettings)

lazy val `example-routeguide-server` = project
  .in(file("modules/examples/routeguide/server"))
  .dependsOn(`example-routeguide-common`)
  .dependsOn(`example-routeguide-runtime`)
  .dependsOn(server)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-routeguide-server")

lazy val `example-routeguide-client` = project
  .in(file("modules/examples/routeguide/client"))
  .dependsOn(`example-routeguide-common`)
  .dependsOn(`example-routeguide-runtime`)
  .dependsOn(`netty`)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-routeguide-client")
  .settings(
    Compile / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / "src" / "main" / "scala-io",
      baseDirectory.value / "src" / "main" / "scala-task"
    )
  )
  .settings(addCommandAlias("runClientIO", "runMain example.routeguide.client.io.ClientAppIO"))
  .settings(
    addCommandAlias("runClientTask", "runMain example.routeguide.client.task.ClientAppTask"))

////////////////////
////  TODOLIST  ////
////////////////////

lazy val `example-todolist-protocol` = project
  .in(file("modules/examples/todolist/protocol"))
  .dependsOn(channel)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-todolist-protocol")

lazy val `example-todolist-runtime` = project
  .in(file("modules/examples/todolist/runtime"))
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-todolist-runtime")

lazy val `example-todolist-server` = project
  .in(file("modules/examples/todolist/server"))
  .dependsOn(`example-todolist-protocol`)
  .dependsOn(`example-todolist-runtime`)
  .dependsOn(server)
  .dependsOn(config)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-todolist-server")
  .settings(exampleTodolistCommonSettings)

lazy val `example-todolist-client` = project
  .in(file("modules/examples/todolist/client"))
  .dependsOn(`example-todolist-protocol`)
  .dependsOn(`example-todolist-runtime`)
  .dependsOn(`netty`)
  .dependsOn(config)
  .settings(coverageEnabled := false)
  .settings(noPublishSettings)
  .settings(moduleName := "mu-rpc-example-todolist-client")
  .settings(exampleTodolistCommonSettings)

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
  .settings(legacyAvroDecimalProtocolSettings)
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

lazy val allModules: Seq[ProjectReference] = Seq(
  common,
  `internal-core`,
  `internal-monix`,
  `internal-fs2`,
  channel,
  monix,
  fs2,
  `client-cache`,
  `netty`,
  `okhttp`,
  server,
  config,
  interceptors,
  `prometheus-shared`,
  `prometheus-client`,
  `prometheus-server`,
  `dropwizard-server`,
  `dropwizard-client`,
  `dropwizard`,
  testing,
  ssl,
  `idlgen-core`,
  `marshallers-jodatime`,
  `example-routeguide-protocol`,
  `example-routeguide-common`,
  `example-routeguide-runtime`,
  `example-routeguide-server`,
  `example-routeguide-client`,
  `example-todolist-protocol`,
  `example-todolist-runtime`,
  `example-todolist-server`,
  `example-todolist-client`,
  `benchmarks-vprev`,
  `benchmarks-vnext`,
  `legacy-avro-decimal-compat-protocol`,
  `legacy-avro-decimal-compat-model`,
  `legacy-avro-decimal-compat-encoders`
)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "mu")
  .settings(noPublishSettings)
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(allModulesDeps: _*)
  .settings(name := "mu-docs")
  .settings(docsSettings: _*)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .enablePlugins(MicrositesPlugin)
