pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val common = project
  .in(file("modules/common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(commonSettings)

lazy val async = project
  .in(file("modules/async"))
  .dependsOn(common % "test->test")
  .settings(moduleName := "frees-rpc-async")
  .settings(asyncSettings)

lazy val internal = project
  .in(file("modules/internal"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-internal")
  .settings(internalSettings)

lazy val client = project
  .in(file("modules/client"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal)
  .dependsOn(async)
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

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(client % "test->test")
  .dependsOn(internal)
  .dependsOn(async)
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-server")
  .settings(serverSettings)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(common % "test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-config")
  .settings(configSettings)

lazy val interceptors = project
  .in(file("modules/interceptors"))
  .settings(moduleName := "frees-rpc-interceptors")
  .settings(interceptorsSettings)

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

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "frees-rpc-testing")
  .settings(testingSettings)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val allModules: Seq[ProjectReference] = Seq(
  common,
  async,
  internal,
  client,
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
  testing
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