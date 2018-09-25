import sbtorgpolicies.model.scalac

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

////////////////
//// COMMON ////
////////////////

lazy val common = project
  .in(file("modules/common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(commonSettings)

lazy val internal = project
  .in(file("modules/internal"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-internal")
  .settings(internalSettings)

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "frees-rpc-testing")
  .settings(testingSettings)

lazy val ssl = project
  .in(file("modules/ssl"))
  .dependsOn(server % "test->test")
  .dependsOn(`client-netty` % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-netty-ssl")
  .settings(nettySslSettings)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(common % "test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-config")
  .settings(configSettings)

////////////////
//// CLIENT ////
////////////////

lazy val client = project
  .in(file("modules/client"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal)
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-client-core")
  .settings(clientCoreSettings)

lazy val `client-netty` = project
  .in(file("modules/client-netty"))
  .dependsOn(client % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-client-netty")
  .settings(clientNettySettings)

lazy val `client-okhttp` = project
  .in(file("modules/client-okhttp"))
  .dependsOn(client % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-client-okhttp")
  .settings(clientOkHttpSettings)

lazy val `client-cache` = project
  .in(file("modules/client-cache"))
  .settings(moduleName := "frees-rpc-client-cache")
  .settings(clientCacheSettings)

////////////////
//// SERVER ////
////////////////

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(client % "test->test")
  .dependsOn(internal % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-server")
  .settings(serverSettings)

//////////////////////
//// INTERCEPTORS ////
//////////////////////

lazy val interceptors = project
  .in(file("modules/interceptors"))
  .settings(moduleName := "frees-rpc-interceptors")
  .settings(interceptorsSettings)

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val `prometheus-shared` = project
  .in(file("modules/prometheus/shared"))
  .dependsOn(interceptors % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-prometheus-shared")
  .settings(prometheusSettings)

lazy val `prometheus-server` = project
  .in(file("modules/prometheus/server"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-prometheus-server")

lazy val `prometheus-client` = project
  .in(file("modules/prometheus/client"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "frees-rpc-prometheus-client")
  .settings(prometheusClientSettings)

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val `dropwizard-server` = project
  .in(file("modules/dropwizard/server"))
  .dependsOn(`prometheus-server` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-dropwizard-server")
  .settings(dropwizardSettings)

lazy val `dropwizard-client` = project
  .in(file("modules/dropwizard/client"))
  .dependsOn(`prometheus-client` % "compile->compile;test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "frees-rpc-dropwizard-client")
  .settings(dropwizardSettings)

////////////////
//// IDLGEN ////
////////////////

lazy val `idlgen-core` = project
  .in(file("modules/idlgen/core"))
  .dependsOn(internal % "compile->compile;test->test")
  .dependsOn(client % "test->test")
  .settings(moduleName := "frees-rpc-idlgen-core")
  .settings(idlGenSettings)

lazy val `idlgen-sbt` = project
  .in(file("modules/idlgen/plugin"))
  .dependsOn(`idlgen-core`)
  .settings(moduleName := "sbt-frees-rpc-idlgen")
  .settings(crossScalaVersions := Seq(scalac.`2.12`))
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "freestyle.rpc.idlgen")
  .enablePlugins(SbtPlugin)

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(client)
  .dependsOn(server)
  .settings(moduleName := "frees-rpc-benchmarks")
  .enablePlugins(JmhPlugin)

//////////////////
//// EXAMPLES ////
//////////////////

////////////////////
//// ROUTEGUIDE ////
////////////////////

lazy val `example-routeguide-protocol` = project
  .in(file("modules/examples/routeguide/protocol"))
  .dependsOn(client)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-routeguide-protocol")

lazy val `example-routeguide-runtime` = project
  .in(file("modules/examples/routeguide/runtime"))
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-routeguide-runtime")
  .settings(exampleRouteguideRuntimeSettings)

lazy val `example-routeguide-common` = project
  .in(file("modules/examples/routeguide/common"))
  .dependsOn(`example-routeguide-protocol`)
  .dependsOn(config)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-routeguide-common")
  .settings(exampleRouteguideCommonSettings)

lazy val `example-routeguide-server` = project
  .in(file("modules/examples/routeguide/server"))
  .dependsOn(`example-routeguide-common`)
  .dependsOn(`example-routeguide-runtime`)
  .dependsOn(server)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-routeguide-server")

lazy val `example-routeguide-client` = project
  .in(file("modules/examples/routeguide/client"))
  .dependsOn(`example-routeguide-common`)
  .dependsOn(`example-routeguide-runtime`)
  .dependsOn(`client-netty`)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-routeguide-client")
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
  .dependsOn(client)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-todolist-protocol")

lazy val `example-todolist-runtime` = project
  .in(file("modules/examples/todolist/runtime"))
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-todolist-runtime")

lazy val `example-todolist-server` = project
  .in(file("modules/examples/todolist/server"))
  .dependsOn(`example-todolist-protocol`)
  .dependsOn(`example-todolist-runtime`)
  .dependsOn(server)
  .dependsOn(config)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-todolist-server")
  .settings(exampleTodolistCommonSettings)

lazy val `example-todolist-client` = project
  .in(file("modules/examples/todolist/client"))
  .dependsOn(`example-todolist-protocol`)
  .dependsOn(`example-todolist-runtime`)
  .dependsOn(`client-netty`)
  .dependsOn(config)
  .settings(noPublishSettings)
  .settings(moduleName := "frees-rpc-example-todolist-client")
  .settings(exampleTodolistCommonSettings)

/////////////////////
//// MARSHALLERS ////
/////////////////////

lazy val `marshallers-jodatime` = project
  .in(file("modules/marshallers/jodatime"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(internal % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-marshallers-jodatime")
  .settings(marshallersJodatimeSettings)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val allModules: Seq[ProjectReference] = Seq(
  common,
  internal,
  client,
  `client-cache`,
  `client-netty`,
  `client-okhttp`,
  server,
  config,
  interceptors,
  `prometheus-shared`,
  `prometheus-client`,
  `prometheus-server`,
  `dropwizard-server`,
  `dropwizard-client`,
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
  benchmarks
)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(noPublishSettings)
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)

lazy val docs = project
  .in(file("docs"))
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)
  .settings(name := "frees-rpc-docs")
  .settings(noPublishSettings)
  .settings(docsSettings)
  .enablePlugins(TutPlugin)
