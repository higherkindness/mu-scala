import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import sbtorgpolicies.runnable.syntax._
import sbtrelease.ReleasePlugin.autoImport._
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls
import tut.TutPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String             = "1.8.4"
      val avrohugger: String         = "1.0.0-RC15"
      val betterMonadicFor: String   = "0.2.4"
      val catsEffect: String         = "1.2.0"
      val circe: String              = "0.11.1"
      val frees: String              = "0.8.2"
      val fs2: String                = "1.0.4"
      val fs2Grpc: String            = "0.4.0-M6"
      val grpc: String               = "1.18.0"
      val jodaTime: String           = "2.10.1"
      val http4s                     = "0.20.0-M6"
      val kindProjector: String      = "0.9.9"
      val log4s: String              = "1.7.0"
      val logback: String            = "1.2.3"
      val monix: String              = "3.0.0-RC2"
      val monocle: String            = "1.5.1-cats"
      val nettySSL: String           = "2.0.20.Final"
      val paradise: String           = "2.1.1"
      val pbdirect: String           = "0.2.1"
      val prometheus: String         = "0.6.0"
      val pureconfig: String         = "0.10.2"
      val reactiveStreams: String    = "1.0.2"
      val scala: String              = "2.12.8"
      val scalacheckToolbox: String  = "0.2.5"
      val scalamockScalatest: String = "3.6.0"
      val scalatest: String          = "3.0.6"
      val skeuomorph: String         = "0.0.8"
      val slf4j: String              = "1.7.26"
      val dropwizard: String         = "4.0.5"
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
        "com.47deg" %% "pbdirect" % V.pbdirect,
        %%("avro4s", V.avro4s),
        %%("log4s", V.log4s),
        "org.scala-lang"         % "scala-compiler" % scalaVersion.value,
        %%("scalamockScalatest") % Test
      )
    )

    lazy val internalMonixSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("monix", V.monix),
        "org.reactivestreams"    % "reactive-streams" % V.reactiveStreams,
        %%("scalamockScalatest") % Test
      )
    )

    lazy val internalFs2Settings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("fs2-core", V.fs2),
        "org.lyranthe.fs2-grpc"  %% "java-runtime" % V.fs2Grpc,
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

    lazy val httpSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("http4s-dsl", V.http4s),
        %%("http4s-blaze-server", V.http4s),
        %%("http4s-circe", V.http4s),
        %%("http4s-blaze-client", V.http4s) % Test,
        %%("circe-generic")                 % Test,
        "ch.qos.logback"                    % "logback-classic" % V.logback % Test
      )
    )

    lazy val configSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("pureconfig", V.pureconfig)
      )
    )

    lazy val prometheusMetricsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("prometheus", V.prometheus)
      )
    )

    lazy val dropwizardMetricsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.dropwizard.metrics" % "metrics-core" % V.dropwizard
      )
    )

    lazy val testingSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-testing", V.grpc),
        %%("cats-effect", V.catsEffect),
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
        %%("monocle-core", V.monocle),
        "io.higherkindness" %% "skeuomorph"      % V.skeuomorph,
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
      micrositeName := "Mu",
      micrositeBaseUrl := "/mu",
      micrositeDescription := "A purely functional library for building RPC endpoint-based services",
      micrositeGithubOwner := "higherkindness",
      micrositeGithubRepo := "mu",
      micrositeGitterChannelUrl := "47deg/mu",
      micrositeOrganizationHomepage := "http://www.47deg.com",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md",
      micrositePushSiteWith := GitHub4s,
      micrositeGithubToken := sys.env.get(orgGithubTokenSetting.value),
      micrositePalette := Map(
        "brand-primary"   -> "#de3423",
        "brand-secondary" -> "#852319",
        "brand-tertiary"  -> "#381C19",
        "gray-dark"       -> "#333333",
        "gray"            -> "#666666",
        "gray-light"      -> "#EDEDED",
        "gray-lighter"    -> "#F4F5F9",
        "white-color"     -> "#E6E7EC"
      )
    )

    lazy val docsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies += %%("scalatest", V.scalatest),
      scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
    )

  }

  import autoImport._

  case class FixedCodecovBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "codecov.io",
        icon = s"http://codecov.io/gh/${info.owner}/${info.repo}/branch/master/graph/badge.svg",
        url = s"http://codecov.io/gh/${info.owner}/${info.repo}"
      ).some
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      description := "mu RPC is a purely functional library for " +
        "building RPC endpoint based services with support for RPC and HTTP/2",
      startYear := Some(2017),
      orgProjectName := "mu",
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
      resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
      addCompilerPlugin(%%("paradise", V.paradise) cross CrossVersion.full),
      addCompilerPlugin(%%("kind-projector", V.kindProjector) cross CrossVersion.binary),
      libraryDependencies ++= Seq(
        %%("scalatest", V.scalatest) % "test",
        %("slf4j-nop", V.slf4j)      % Test
      )
    ) ++ Seq(
      // sbt-org-policies settings:
      // format: OFF
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        FixedCodecovBadge.apply,
        { info => MavenCentralBadge.apply(info.copy(libName = "mu")) },
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
      orgAfterCISuccessTaskListSetting := List(
        orgPublishReleaseTask.asRunnableItem(allModules = true, aggregated = false, crossScalaVersions = true),
        orgUpdateDocFiles.asRunnableItem
      ) ++ guard(!version.value.endsWith("-SNAPSHOT"))(defaultPublishMicrosite)
    )
  // format: ON
}
