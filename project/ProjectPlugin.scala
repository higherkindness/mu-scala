import freestyle.FreestylePlugin
import freestyle.FreestylePlugin.autoImport._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.templates.badges._
import sbtrelease.ReleasePlugin.autoImport._

import scala.language.reflectiveCalls
import tut.TutPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String             = "1.8.3"
      val avrohugger: String         = "1.0.0-RC10"
      val betterMonadicFor: String   = "0.2.4"
      val catsEffect: String         = "0.10.1"
      val circe: String              = "0.9.3"
      val frees: String              = "0.8.1"
      val fs2: String                = "0.10.5"
      val fs2ReactiveStreams: String = "0.5.1"
      val grpc: String               = "1.11.0"
      val http4s                     = "0.18.9"
      val log4s: String              = "1.6.1"
      val logback: String            = "1.2.3"
      val monix: String              = "3.0.0-RC1"
      val nettySSL: String           = "2.0.8.Final"
      val pbdirect: String           = "0.1.0"
      val prometheus: String         = "0.3.0"
      val monocle: String            = "1.5.0-cats"
      val scalacheck: String         = "1.14.0"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-effect", V.catsEffect) % Test,
        %%("scalamockScalatest")        % Test,
        %%("scheckToolboxDatetime")     % Test
      )
    )

    lazy val internalSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-effect", V.catsEffect),
        %("grpc-stub", V.grpc),
        %%("monix", V.monix),
        %%("monocle-core", V.monocle),
        %%("fs2-reactive-streams", V.fs2ReactiveStreams),
        %%("fs2-core", V.fs2),

        %%("http4s-dsl", V.http4s),
        %%("http4s-blaze-server", V.http4s),
        %%("http4s-circe", V.http4s),
        %%("circe-generic"),
        %%("http4s-blaze-client", V.http4s),

        %%("pbdirect", V.pbdirect),
        %%("avro4s", V.avro4s),
        %%("log4s", V.log4s),
        "org.scala-lang"         % "scala-compiler" % scalaVersion.value,
        %%("scalamockScalatest") % Test
      )
    )

    lazy val clientCoreSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-effect", V.catsEffect),
        %%("scalamockScalatest") % Test,
        %("grpc-netty", V.grpc)  % Test
      )
    )

    lazy val clientNettySettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc),
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL % Test
      )
    )

    lazy val clientOkHttpSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-okhttp", V.grpc)
      )
    )

    lazy val clientCacheSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("log4s", V.log4s),
        %%("fs2-core", V.fs2),
        %%("cats-effect", V.catsEffect),
        compilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)
      )
    )

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc),
        %%("scalamockScalatest") % Test,
        "io.netty"               % "netty-tcnative-boringssl-static" % V.nettySSL % Test
      )
    )

    lazy val configSettings = Seq(
      libraryDependencies ++= Seq(
        %%("pureconfig")
      )
    )

    lazy val interceptorsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-core", V.grpc)
      )
    )

    lazy val prometheusSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("prometheus", V.prometheus)
      )
    )

    lazy val prometheusClientSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc) % Test
      )
    )

    lazy val dropwizardSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.prometheus" % "simpleclient_dropwizard" % V.prometheus
      )
    )

    lazy val testingSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-testing", V.grpc),
        %%("scalacheck") % Test
      )
    )

    lazy val nettySslSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL
      )
    )

    lazy val idlGenSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "com.julianpeeters" %% "avrohugger-core" % V.avrohugger,
        %%("circe-generic", V.circe)
      )
    )

    lazy val exampleRouteguideRuntimeSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("monix", V.monix)
      )
    )

    lazy val exampleRouteguideCommonSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("circe-core", V.circe),
        %%("circe-generic", V.circe),
        %%("circe-parser", V.circe),
        %%("log4s", V.log4s),
        %("logback-classic", V.logback)
      )
    )

    lazy val exampleTodolistRuntimeSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("monix", V.monix)
      )
    )

    lazy val exampleTodolistCommonSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.frees" %% "frees-todolist-lib" % V.frees,
        %%("log4s", V.log4s),
        %("logback-classic", V.logback)
      )
    )

    lazy val sbtPluginSettings: Seq[Def.Setting[_]] = Seq(
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dversion=" + version.value
          )
      },
      // Custom release process for the plugin:
      releaseProcess := Seq[ReleaseStep](
        releaseStepCommandAndRemaining("^ publishSigned"),
        ReleaseStep(action = "sonatypeReleaseAll" :: _)
      )
    )

    lazy val rpcHttpServerSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("http4s-dsl", V.http4s),
        %%("http4s-blaze-server", V.http4s),
        %%("http4s-circe", V.http4s),
        %%("circe-generic"),
        %%("http4s-blaze-client", V.http4s) % Test,
        %%("scalacheck", V.scalacheck) % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
      )
    )

    lazy val docsSettings = Seq(
      // Pointing to https://github.com/frees-io/freestyle/tree/master/docs/src/main/tut/docs/rpc
      tutTargetDirectory := baseDirectory.value.getParentFile.getParentFile / "docs" / "src" / "main" / "tut" / "docs" / "rpc",
      libraryDependencies ++= Seq(%%("scalamockScalatest") % "tut")
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    // format: OFF
    sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      addCompilerPlugin(%%("paradise") cross CrossVersion.full),
      libraryDependencies ++= commonDeps :+ %("slf4j-nop") % Test,
      scalaVersion := "2.12.6",
      crossScalaVersions := Seq("2.11.12", "2.12.6"),
      Test / fork := true,
      Tut / scalacOptions -= "-Ywarn-unused-import",
      orgAfterCISuccessTaskListSetting ~= (_.filterNot(_ == defaultPublishMicrosite)),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply,
        { info => MavenCentralBadge.apply(info.copy(libName = "frees-rpc")) },
        ScalaLangBadge.apply,
        LicenseBadge.apply,
        // Gitter badge (owner field) can be configured with default value if we migrate it to the frees-io organization
        { info => GitterBadge.apply(info.copy(owner = "47deg", repo = "freestyle")) },
        GitHubIssuesBadge.apply
      )
    )
  // format: ON
}
