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