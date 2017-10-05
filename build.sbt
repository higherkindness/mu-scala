pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(common, rpc, protogen)

lazy val common = project
  .in(file("common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(scalaMetaSettings: _*)

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "frees-rpc")
  .dependsOn(common)
  .aggregate(common)
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps() ++
        Seq(
          %("grpc-all"),
          %%("freestyle-async"),
          %%("freestyle-config"),
          %%("freestyle-logging"),
          %%("pbdirect"),
          %%("monix"),
          %%("scalamockScalatest") % "test"
        )
    ): _*
  )

lazy val protogen = project
  .in(file("protogen"))
  .settings(moduleName := "sbt-frees-protogen")
  .dependsOn(common)
  .aggregate(common)
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      sbtPlugin := true,
      scalaVersion := sbtorgpolicies.model.scalac.`2.12`,
      crossScalaVersions := Seq(sbtorgpolicies.model.scalac.`2.12`),
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps() ++ Seq(
        %%("scalameta-contrib", "1.8.0")
      )
    ): _*
  )