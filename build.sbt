import com.trueaccord.scalapb.compiler.{Version => cv}

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(rpc, googleApi, demo)

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "freestyle-rpc")
  .settings(
    Seq(
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps()
    ): _*
  )

lazy val GOPATH = Option(sys.props("go.path")).getOrElse("/your/go/path")

lazy val googleApi = project
  .in(file("third_party"))
  .settings(
    PB.protoSources.in(Compile) ++= Seq(
      file(s"$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis/")
    ),
    PB.targets.in(Compile) := Seq(scalapb.gen() -> sourceManaged.in(Compile).value),
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % cv.scalapbVersion % "protobuf"
  )

lazy val demo = project
  .in(file("demo"))
  .aggregate(rpc, googleApi)
  .dependsOn(rpc, googleApi)
  .settings(moduleName := "freestyle-rpc-demo")
  .settings(noPublishSettings: _*)
  .settings(commandAliases: _*)
  .settings(
    Seq(
      PB.protocOptions.in(Compile) ++= Seq(
        "-I/usr/local/include -I.",
        s"-I$GOPATH/src",
        s"-I$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis",
        "--go_out=plugins=grpc:./demo/gateway",
        "--grpc-gateway_out=logtostderr=true:./demo/gateway",
        "--swagger_out=logtostderr=true:./demo/gateway"
      ),
      PB.protoSources.in(Compile) ++= Seq(sourceDirectory.in(Compile).value / "proto"),
      PB.targets.in(Compile) := Seq(scalapb.gen() -> sourceManaged.in(Compile).value),
      libraryDependencies ++= Seq(
        "io.grpc"                % "grpc-all"              % cv.grpcJavaVersion,
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
