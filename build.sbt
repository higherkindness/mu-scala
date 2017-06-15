pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project.in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(`freestyle-rpcJS`, `freestyle-rpcJVM`)

lazy val `freestyle-rpc` = crossProject.in(file("freestyle-rpc"))
  .settings(moduleName := "freestyle-rpc")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ freestyleCoreDeps(): _*)

lazy val `freestyle-rpcJVM` = `freestyle-rpc`.jvm
lazy val `freestyle-rpcJS`  = `freestyle-rpc`.js
