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
      val avrohugger: String         = "1.0.0-RC4"
      val catsEffect: String         = "0.10"
      val circe: String              = "0.9.3"
      val frees: String              = "0.8.0"
      val fs2ReactiveStreams: String = "0.5.1"
      val grpc: String               = "1.11.0"
      val log4s: String              = "1.6.1"
      val logback: String            = "1.2.3"
      val nettySSL: String           = "2.0.8.Final"
      val pbdirect: String           = "0.1.0"
      val prometheus: String         = "0.3.0"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      scalacOptions := Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked",
        "-language:higherKinds"),
      libraryDependencies ++= Seq(
        %%("cats-effect", V.catsEffect) % Test,
        %%("scalamockScalatest")        % Test
      )
    )

    lazy val internalSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("frees-async-cats-effect", V.frees),
        %%("frees-async-guava", V.frees) exclude ("com.google.guava", "guava"),
        %("grpc-stub", V.grpc),
        %%("monix"),
        %%("fs2-reactive-streams", V.fs2ReactiveStreams),
        %%("pbdirect", V.pbdirect),
        %%("avro4s", V.avro4s),
        %%("log4s", V.log4s),
        %%("scalamockScalatest") % Test
      )
    )

    lazy val clientCoreSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("frees-async-cats-effect", V.frees),
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

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc),
        %%("scalamockScalatest") % Test,
        "io.netty"               % "netty-tcnative-boringssl-static" % V.nettySSL % Test
      )
    )

    lazy val configSettings = Seq(
      libraryDependencies ++= Seq(
        %%("frees-config", V.frees)
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
        %%("monix")
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

    lazy val docsSettings = Seq(
      // Pointing to https://github.com/frees-io/freestyle/tree/master/docs/src/main/tut/docs/rpc
      tutTargetDirectory := baseDirectory.value.getParentFile.getParentFile / "docs" / "src" / "main" / "tut" / "docs" / "rpc"
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    // format: OFF
    scalaMetaSettings ++ sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      libraryDependencies ++= commonDeps :+ %("slf4j-nop") % Test,
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
