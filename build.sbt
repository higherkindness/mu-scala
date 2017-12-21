import sbtorgpolicies.templates.badges._

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(noPublishSettings)
  .settings(
    orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply,
        { info => MavenCentralBadge.apply(info.copy(libName = "frees")) },
        ScalaLangBadge.apply,
        LicenseBadge.apply,
        // Gitter badge (owner field) can be configured with default value if we migrate it to the frees-io organization
        { info => GitterBadge.apply(info.copy(owner = "47deg", repo = "freestyle")) },
        GitHubIssuesBadge.apply
    )
  )
  .dependsOn(common, rpc, docs)
  .aggregate(common, rpc, docs)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(common, rpc)
  .aggregate(common, rpc)
  .settings(name := "frees-rpc-docs")
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    addCompilerPlugin(%%("scalameta-paradise") cross CrossVersion.full),
    libraryDependencies += %%("scalameta", "1.8.0"),
    scalacOptions += "-Xplugin-require:macroparadise",
    scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
  )
  .enablePlugins(MicrositesPlugin)

lazy val common = project
  .in(file("common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(scalacOptions := Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked"))

lazy val V = new {
    val frees: String = "0.5.0"
    val grpc: String = "1.7.1"
}

lazy val rpc = project
  .in(file("rpc"))
  .dependsOn(common)
  .settings(moduleName := "frees-rpc")
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      scalacOptions += "-Ywarn-unused-import",
      libraryDependencies ++= commonDeps ++
        Seq(
          %%("frees-core", V.frees),
          %%("frees-async", V.frees),
          %%("frees-async-guava", V.frees) exclude ("com.google.guava", "guava"),
          %%("frees-async-cats-effect", V.frees),
          %%("frees-config", V.frees),
          %%("frees-logging", V.frees),
          %("grpc-all", V.grpc),
          %%("monix"),
          %%("pbdirect", "0.0.7"),
          "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0",
          %%("scalameta-contrib", "1.8.0"),
          %("grpc-testing", V.grpc) % Test,
          %%("scalatest")          % Test,
          %%("scalamockScalatest") % Test
        )
    ): _*
  )
