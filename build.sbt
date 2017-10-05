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
          %("grpc-all"),
          %%("freestyle-async"),
          %%("freestyle-config"),
          %%("freestyle-logging"),
          %%("scalameta-contrib", "1.8.0"),
          %%("pbdirect"),
          %%("monix"),
          %%("scalamockScalatest") % "test"
        )
    ): _*
  )