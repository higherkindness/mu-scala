pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val common = project
  .in(file("modules/common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(commonSettings)

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(common)
  .settings(moduleName := "frees-rpc-core")
  .settings(coreSettings)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-config")
  .settings(configSettings)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val allModules: Seq[ProjectReference] = Seq(
  common,
  core,
  config
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