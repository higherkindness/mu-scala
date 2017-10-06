pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

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
  .settings(scalacOptions ~= (_ filterNot (_ == "-Xplugin-require:macroparadise")))
  .settings(sbtPlugin := true)