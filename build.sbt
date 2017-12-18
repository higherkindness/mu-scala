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
  .dependsOn(common, rpc)
  .aggregate(common, rpc)

lazy val common = project
  .in(file("common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(scalacOptions := Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked"))

lazy val freesV = "0.4.6"

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
          %%("frees-core", freesV),
          %%("frees-async", freesV),
          %%("frees-async-guava", freesV),
          %%("frees-async-cats-effect", freesV),
          %%("frees-config", freesV),
          %%("frees-logging", freesV),
          %%("frees-tagless", freesV),
          %("grpc-all", "1.7.0"),
          %%("monix"),
          %%("pbdirect", "0.0.7"),
          "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0",
          %%("scalameta-contrib", "1.8.0"),
          %("grpc-testing")        % Test,
          %%("scalatest")          % Test,
          %%("scalamockScalatest") % Test
        )
    ): _*
  )
