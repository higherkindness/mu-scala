import com.trueaccord.scalapb.compiler.{Version => cv}

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(rpc, `demo-greeting`, `demo-protocolgen`)

lazy val rpc = project
  .in(file("rpc"))
  .settings(moduleName := "freestyle-rpc")
  .settings(
    Seq(
      libraryDependencies ++= commonDeps ++ freestyleCoreDeps() ++
        Seq(
          %%("freestyle-async"),
          %%("freestyle-config"),
          %%("scalameta-contrib"),
          "io.grpc"                % "grpc-all" % "1.4.0",
          %%("scalamockScalatest") % "test"
        )
    ): _*
  )

lazy val protogen = taskKey[Unit]("Generates .proto files from freestyle-rpc service definitions")

lazy val `demo-protocolgen` = project
  .in(file("demo/protocolgen"))
  .settings(moduleName := "freestyle-rpc-demo-protocolgen")
  .aggregate(rpc)
  .dependsOn(rpc)
  .settings(noPublishSettings: _*)
  .settings(commandAliases: _*)
  .settings(demoCommonSettings: _*)
  .settings(
    protogen := {
      toError(
        (runner in Compile).value
          .run(
            mainClass = "freestyle.rpc.protocol.ProtoCodeGen",
            classpath = sbt.Attributed.data((fullClasspath in Compile).value),
            options = Seq(
              (baseDirectory.value / "src" / "main" / "scala").absolutePath,
              (baseDirectory.value / "src" / "main" / "proto").absolutePath
            ),
            log = streams.value.log
          )
      )
    }
  )

lazy val `demo-greeting` = project
  .in(file("demo/greeting"))
  .settings(moduleName := "freestyle-rpc-demo-greeting")
  .aggregate(rpc)
  .dependsOn(rpc)
  .settings(noPublishSettings: _*)
  .settings(commandAliases: _*)
  .settings(demoCommonSettings: _*)
  .settings(
    Seq(
      libraryDependencies ++= Seq(
        %%("freestyle-async"),
        %%("freestyle-config")
      )
    ): _*)

lazy val googleApi = project
  .in(file("third_party"))
  .settings(
    PB.protoSources.in(Compile) ++= Seq(
      file(s"$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis/")
    ),
    PB.targets.in(Compile) := Seq(scalapb.gen() -> sourceManaged.in(Compile).value),
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % cv.scalapbVersion % "protobuf"
  )

lazy val `demo-http` = project
  .in(file("demo/http"))
  .settings(moduleName := "freestyle-rpc-demo-http")
  .aggregate(rpc, googleApi, `demo-greeting`)
  .dependsOn(rpc, googleApi, `demo-greeting`)
  .settings(noPublishSettings: _*)
  .settings(demoCommonSettings: _*)
  .settings(
    Seq(
      PB.protocOptions.in(Compile) ++= Seq(
        "-I/usr/local/include -I.",
        s"-I$GOPATH/src",
        s"-I$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis",
        "--go_out=plugins=grpc:./demo/http/gateway",
        "--grpc-gateway_out=logtostderr=true:./demo/http/gateway",
        "--swagger_out=logtostderr=true:./demo/http/gateway"
      )
    ): _*
  )
