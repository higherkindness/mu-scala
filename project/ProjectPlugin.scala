import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.SetSetting
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import sbtrelease.ReleasePlugin.autoImport._
import scoverage.ScoverageKeys
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls
import tut.TutPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String             = "1.8.3"
      val avrohugger: String         = "1.0.0-RC14"
      val betterMonadicFor: String   = "0.2.4"
      val catsEffect: String         = "0.10.1"
      val circe: String              = "0.9.3"
      val frees: String              = "0.8.2"
      val fs2: String                = "0.10.6"
      val fs2ReactiveStreams: String = "0.5.1"
      val jodaTime: String           = "2.10"
      val grpc: String               = "1.15.0"
      val log4s: String              = "1.6.1"
      val logback: String            = "1.2.3"
      val monix: String              = "3.0.0-RC1"
      val monocle: String            = "1.5.1-cats"
      val nettySSL: String           = "2.0.12.Final"
      val paradise: String           = "2.1.1"
      val pbdirect: String           = "0.1.0"
      val prometheus: String         = "0.5.0"
      val pureconfig: String         = "0.9.2"
      val scala: String              = "2.12.7"
      val scalacheckToolbox: String  = "0.2.5"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-effect", V.catsEffect)                  % Test,
        %%("scalamockScalatest")                         % Test,
        %%("scheckToolboxDatetime", V.scalacheckToolbox) % Test
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
        %%("pureconfig", V.pureconfig)
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

    lazy val marshallersJodatimeSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("joda-time", V.jodaTime),
        %%("scheckToolboxDatetime", V.scalacheckToolbox) % Test
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

    lazy val crossSettings: Seq[Def.Setting[_]] = Seq(
      unmanagedSourceDirectories in Compile += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala"
      },
      unmanagedSourceDirectories in Test += {
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
      }
    )

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName := "Mu-rpc",
      micrositeBaseUrl := "/mu-rpc",
      micrositeDescription := "A purely functional library for building RPC endpoint-based services",
      micrositeDocumentationUrl := "docs/rpc/core-concepts.html",
      micrositeGithubOwner := "higherkindness",
      micrositeGithubRepo := "mu-rpc",
      micrositeGitterChannelUrl := "47deg/mu",
      micrositeOrganizationHomepage := "http://www.47deg.com",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md",
      micrositePushSiteWith := GitHub4s
    )

    lazy val legacyAvroDecimalProtocolSettings: Seq[Def.Setting[_]] = Seq(
      publishMavenStyle := true,
      crossPaths := false,
      libraryDependencies := Nil
    )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      description := "mu RPC is a purely functional library for " +
        "building RPC endpoint based services with support for RPC and HTTP/2",
      startYear := Some(2017),
      orgProjectName := "mu-rpc",
      orgGithubSetting := GitHubSettings(
        organization = "higherkindness",
        project = (name in LocalRootProject).value,
        organizationName = "47 Degrees",
        groupId = "io.higherkindness",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      scalaVersion := V.scala,
      crossScalaVersions := Seq("2.11.12", V.scala),
      scalacOptions ++= scalacAdvancedOptions,
      scalacOptions ~= (_ filterNot Set("-Yliteral-types", "-Xlint").contains),
      Test / fork := true,
      Tut / scalacOptions -= "-Ywarn-unused-import",
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false,
      addCompilerPlugin(%%("paradise", V.paradise) cross CrossVersion.full),
      libraryDependencies ++= Seq(
        %%("scalatest") % "test",
        %("slf4j-nop")  % Test
      )
    ) ++ Seq(
      // sbt-org-policies settings:
      // format: OFF
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply, { info => MavenCentralBadge.apply(info.copy(libName = "mu-rpc")) },
        ScalaLangBadge.apply,
        LicenseBadge.apply,
        // Gitter badge (owner field) can be configured with default value if we migrate it to the higherkindness organization
        { info => GitterBadge.apply(info.copy(owner = "47deg", repo = "mu")) },
        GitHubIssuesBadge.apply
      ),
      orgEnforcedFilesSetting := List(
        LicenseFileType(orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
        ContributingFileType(
          orgProjectName.value,
          // Organization field can be configured with default value if we migrate it to the higherkindness organization
          orgGithubSetting.value.copy(organization = "47deg", project = "mu")
        ),
        AuthorsFileType(
          name.value,
          orgGithubSetting.value,
          orgMaintainersSetting.value,
          orgContributorsSetting.value),
        NoticeFileType(orgProjectName.value, orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
        VersionSbtFileType,
        ChangelogFileType,
        ReadmeFileType(
          orgProjectName.value,
          orgGithubSetting.value,
          startYear.value,
          orgLicenseSetting.value,
          orgCommitBranchSetting.value,
          sbtPlugin.value,
          name.value,
          version.value,
          scalaBinaryVersion.value,
          sbtBinaryVersion.value,
          orgSupportedScalaJSVersion.value,
          orgBadgeListSetting.value
        ),
        ScalafmtFileType,
        TravisFileType(crossScalaVersions.value, orgScriptCICommandKey, orgAfterCISuccessCommandKey)
      ),
      orgScriptTaskListSetting := List(
        (clean in Global).asRunnableItemFull,
        SetSetting(coverageEnabled in Global, true).asRunnableItem,
        (compile in Compile).asRunnableItemFull,
        (test in Test).asRunnableItemFull,
        (ScoverageKeys.coverageReport in Test).asRunnableItemFull,
        (ScoverageKeys.coverageAggregate in Test).asRunnableItemFull,
        SetSetting(coverageEnabled in Global, false).asRunnableItem,
        "docs/tut".asRunnableItem,
      )
    )
  // format: ON
}
