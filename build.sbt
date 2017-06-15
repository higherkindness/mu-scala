import com.trueaccord.scalapb.compiler.{Version => cv}

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(rpc, demo)

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "freestyle-rpc")
  .settings(
    Seq(
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps()
    ): _*
  )

lazy val demo = project
  .in(file("demo"))
  .aggregate(rpc)
  .dependsOn(rpc)
  .settings(moduleName := "freestyle-rpc-demo")
  .settings(noPublishSettings: _*)
  .settings(commandAliases: _*)
  .settings(
    Seq(
      PB.protoSources.in(Compile) := Seq(sourceDirectory.in(Compile).value / "proto"),
      PB.targets.in(Compile) := Seq(scalapb.gen() -> sourceManaged.in(Compile).value),
      libraryDependencies ++= Seq(
        "io.grpc"                % "grpc-netty"            % cv.grpcJavaVersion,
        "com.trueaccord.scalapb" %% "scalapb-runtime"      % cv.scalapbVersion % "protobuf",
        "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % cv.scalapbVersion
      )
    ): _*
  )

lazy val commandAliases: Seq[Def.Setting[_]] =
  addCommandAlias(
    "runServer",
    ";project demo;runMain freestyle.rpc.demo.greeting.GreetingServerApp") ++
    addCommandAlias(
      "runClient",
      ";project demo;runMain freestyle.rpc.demo.greeting.GreetingClientApp")
