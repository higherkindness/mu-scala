pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "frees-rpc")
  .settings(scalaMetaSettings: _*)
  .settings(
    Seq(
      resolvers += Resolver.bintrayRepo("beyondthelines", "maven"),
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps() ++
        Seq(
          %%("freestyle-async"),
          %%("freestyle-config"),
          %%("scalameta-contrib"),
          "io.grpc"        % "grpc-all"  % "1.6.1",
          "beyondthelines" %% "pbdirect" % "0.0.3",
          %%("monix"),
          %%("scalamockScalatest") % "test"
        ),
      coverageExcludedPackages := "<empty>;freestyle\\.rpc\\.demo\\..*"
    ): _*
  )