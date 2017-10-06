import sbtorgpolicies.model._

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(rpc)

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "frees-rpc")
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps() ++
        Seq(
          %%("freestyle-async"),
          %%("freestyle-config"),
          %%("freestyle-logging"),
          %("grpc-all"),
          %%("monix"),
          %%("pbdirect"),
          %%("scalameta-contrib", "1.8.0"),
          %%("scalamockScalatest") % "test"
        )
    ): _*
  )

lazy val protogen = project
  .in(file("protogen"))
  .settings(moduleName := "sbt-frees-protogen")
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      sbtPlugin := true,
      scalaVersion := scalac.`2.12`,
      crossScalaVersions := Seq(scalac.`2.10`, scalac.`2.12`),
      crossSbtVersions := Seq(sbtV.`0.13`, sbtV.`1.0`)
    ): _*
  )