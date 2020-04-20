import microsites.MicrositesPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import sbtorgpolicies.runnable.syntax._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls
import mdoc.MdocPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String                = "3.0.9"
      val betterMonadicFor: String      = "0.3.1"
      val catsEffect: String            = "2.1.3"
      val circe: String                 = "0.13.0"
      val dockerItScala                 = "0.9.9"
      val dropwizard: String            = "4.1.6"
      val embeddedKafka: String         = "2.4.1.1"
      val enumeratum: String            = "1.5.15"
      val fs2: String                   = "2.3.0"
      val fs2Grpc: String               = "0.7.0"
      val fs2Kafka: String              = "0.20.2"
      val grpc: String                  = "1.28.1"
      val jodaTime: String              = "2.10.5"
      val http4s: String                = "0.21.3"
      val kindProjector: String         = "0.10.3"
      val lastRelease                   = "0.21.3"
      val log4cats: String              = "1.0.1"
      val log4s: String                 = "1.8.2"
      val logback: String               = "1.2.3"
      val monix: String                 = "3.1.0"
      val natchez: String               = "0.0.11"
      val nettySSL: String              = "2.0.28.Final"
      val paradise: String              = "2.1.1"
      val pbdirect: String              = "0.5.1"
      val prometheus: String            = "0.8.1"
      val pureconfig: String            = "0.12.3"
      val reactiveStreams: String       = "1.0.3"
      val scala212: String              = "2.12.10"
      val scala213: String              = "2.13.1"
      val scalaCollectionCompat: String = "2.1.4"
      val scalacheck: String            = "1.14.3"
      val scalacheckToolbox: String     = "0.3.5"
      val scalamock: String             = "4.4.0"
      val scalatest: String             = "3.1.1"
      val scalatestplusScheck: String   = "3.1.0.0-RC2"
      val slf4j: String                 = "1.7.30"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel"     %% "cats-effect"                 % V.catsEffect          % Test,
        "org.scalamock"     %% "scalamock"                   % V.scalamock           % Test,
        "com.47deg"         %% "scalacheck-toolbox-datetime" % V.scalacheckToolbox   % Test,
        "org.scalatestplus" %% "scalatestplus-scalacheck"    % V.scalatestplusScheck % Test
      )
    )

    lazy val macroSettings: Seq[Setting[_]] = {

      def paradiseDependency(sv: String): Seq[ModuleID] =
        if (isOlderScalaVersion(sv)) {
          Seq(
            compilerPlugin(
              ("org.scalamacros" % "paradise" % V.paradise).cross(CrossVersion.patch)
            )
          )
        } else Seq.empty

      def macroAnnotationScalacOption(sv: String): Seq[String] =
        if (isOlderScalaVersion(sv)) Seq.empty
        else Seq("-Ymacro-annotations")

      Seq(
        libraryDependencies ++= Seq(
          scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided
        ) ++ paradiseDependency(scalaVersion.value),
        scalacOptions ++= macroAnnotationScalacOption(scalaVersion.value)
      )
    }

    lazy val internalCoreSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel"          %% "cats-effect"             % V.catsEffect,
        "io.grpc"                % "grpc-stub"                % V.grpc,
        "com.47deg"              %% "pbdirect"                % V.pbdirect,
        "com.beachape"           %% "enumeratum"              % V.enumeratum,
        "com.sksamuel.avro4s"    %% "avro4s-core"             % V.avro4s,
        "org.log4s"              %% "log4s"                   % V.log4s,
        "org.tpolecat"           %% "natchez-core"            % V.natchez,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat
      ),
      // Disable this flag because quasiquotes trigger a lot of false positive warnings
      scalacOptions -= "-Wunused:patvars",    // for Scala 2.13
      scalacOptions -= "-Ywarn-unused:params" // for Scala 2.12
    )

    lazy val internalMonixSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.monix"            %% "monix"           % V.monix,
        "org.reactivestreams" % "reactive-streams" % V.reactiveStreams
      )
    )

    lazy val internalFs2Settings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "co.fs2"                %% "fs2-core"     % V.fs2,
        "org.lyranthe.fs2-grpc" %% "java-runtime" % V.fs2Grpc
      )
    )

    lazy val clientCoreSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect" % V.catsEffect,
        "io.grpc"       % "grpc-netty"   % V.grpc % Test
      )
    )

    lazy val clientNettySettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc"  % "grpc-netty"                      % V.grpc,
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL % Test
      )
    )

    lazy val clientOkHttpSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-okhttp" % V.grpc
      )
    )

    lazy val clientCacheSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.log4s"     %% "log4s"       % V.log4s,
        "co.fs2"        %% "fs2-core"    % V.fs2,
        "org.typelevel" %% "cats-effect" % V.catsEffect,
        compilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)
      )
    )

    lazy val healthCheckSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect" % V.catsEffect
      )
    )

    lazy val healthCheckSettingsFS2: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.chrisdavenport" %% "log4cats-core"  % V.log4cats,
        "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats,
        "co.fs2"            %% "fs2-core"       % V.fs2,
        "org.typelevel"     %% "cats-effect"    % V.catsEffect
      )
    )

    lazy val healthCheckSettingsMonix: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.chrisdavenport" %% "log4cats-core"  % V.log4cats,
        "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats,
        "io.monix"          %% "monix"          % V.monix,
        "org.typelevel"     %% "cats-effect"    % V.catsEffect
      )
    )

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc"  % "grpc-netty"                      % V.grpc,
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL % Test
      )
    )

    lazy val httpSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.http4s"     %% "http4s-dsl"          % V.http4s,
        "org.http4s"     %% "http4s-blaze-server" % V.http4s,
        "org.http4s"     %% "http4s-circe"        % V.http4s,
        "org.http4s"     %% "http4s-blaze-client" % V.http4s % Test,
        "io.circe"       %% "circe-generic"       % V.circe % Test,
        "ch.qos.logback" % "logback-classic"      % V.logback % Test
      )
    )

    lazy val configSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "com.github.pureconfig" %% "pureconfig" % V.pureconfig
      )
    )

    lazy val prometheusMetricsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.prometheus" % "simpleclient" % V.prometheus
      )
    )

    lazy val dropwizardMetricsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.dropwizard.metrics" % "metrics-core" % V.dropwizard
      )
    )

    lazy val testingSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc"           % "grpc-testing"              % V.grpc,
        "org.typelevel"     %% "cats-effect"              % V.catsEffect,
        "org.scalacheck"    %% "scalacheck"               % V.scalacheck % Test,
        "org.scalatestplus" %% "scalatestplus-scalacheck" % V.scalatestplusScheck % Test
      )
    )

    lazy val nettySslSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL
      )
    )

    lazy val kafkaSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "com.ovoenergy"           %% "fs2-kafka"      % V.fs2Kafka,
        "io.github.embeddedkafka" %% "embedded-kafka" % V.embeddedKafka % Test
      )
    )

    lazy val marshallersJodatimeSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "joda-time" % "joda-time"                    % V.jodaTime,
        "com.47deg" %% "scalacheck-toolbox-datetime" % V.scalacheckToolbox % Test
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

    lazy val noCrossCompilationLastScala: Seq[Def.Setting[_]] = Seq(
      scalaVersion := V.scala212,
      crossScalaVersions := Seq(V.scala212)
    )

    lazy val compatSettings: Seq[Def.Setting[_]] = Seq(
      unmanagedSourceDirectories in Compile += {
        val base = baseDirectory.value / "src" / "main"
        val dir  = if (isOlderScalaVersion(scalaVersion.value)) "scala-2.13-" else "scala-2.13+"

        base / dir
      }
    )

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName := "Mu-Scala",
      micrositeBaseUrl := "mu-scala",
      micrositeDescription := "A purely functional library for building RPC endpoint-based services",
      micrositeGithubOwner := "higherkindness",
      micrositeGithubRepo := "mu-scala",
      micrositeGitterChannelUrl := "47deg/mu",
      micrositeOrganizationHomepage := "https://www.47deg.com",
      micrositeCompilingDocsTool := WithMdoc,
      micrositePushSiteWith := GitHub4s,
      mdocIn := (sourceDirectory in Compile).value / "docs",
      micrositeGithubToken := sys.env.get(orgGithubTokenSetting.value),
      micrositePalette := Map(
        "brand-primary"   -> "#001e38",
        "brand-secondary" -> "#F44336",
        "white-color"     -> "#E6E7EC"
      ),
      micrositeHighlightTheme := "github-gist",
      micrositeHighlightLanguages += "protobuf"
    )

    lazy val mdocSettings = Seq(
      scalacOptions ~= (_ filterNot Set("-Xfatal-warnings", "-Ywarn-unused-import", "-Xlint").contains)
    )

    lazy val docsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.scalatest"         %% "scalatest"                % V.scalatest,
        "org.scalatestplus"     %% "scalatestplus-scalacheck" % V.scalatestplusScheck,
        "io.dropwizard.metrics" % "metrics-jmx"               % V.dropwizard,
        "org.tpolecat"          %% "natchez-jaeger"           % V.natchez
      )
    ) ++ mdocSettings

    lazy val haskellIntegrationTestSettings = Seq(
      publishArtifact := false,
      Test / parallelExecution := false,
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest"                   % V.scalatest     % Test,
        "com.whisk"     %% "docker-testkit-scalatest"    % V.dockerItScala % Test,
        "com.whisk"     %% "docker-testkit-impl-spotify" % V.dockerItScala % Test
      )
    )

    def isOlderScalaVersion(sv: String): Boolean =
      CrossVersion.partialVersion(sv) match {
        case Some((2, minor)) if minor < 13 => true
        case _                              => false
      }

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
    Seq(
      description := "mu RPC is a purely functional library for " +
        "building RPC endpoint based services with support for RPC and HTTP/2",
      startYear := Some(2017),
      orgProjectName := "mu-scala",
      orgGithubSetting := GitHubSettings(
        organization = "higherkindness",
        project = (name in LocalRootProject).value,
        organizationName = "47 Degrees",
        groupId = "io.higherkindness",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      scalaVersion := V.scala213,
      crossScalaVersions := Seq(V.scala212, V.scala213),
      scalacOptions --= Seq("-Xfuture", "-Xfatal-warnings"),
      Test / fork := true,
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false,
      addCompilerPlugin(
        "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.binary
      ),
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % V.scalatest % Test,
        "org.slf4j"     % "slf4j-nop"  % V.slf4j     % Test
      ),
      releaseProcess := Seq[ReleaseStep](
        orgInitialVcsChecks,
        checkSnapshotDependencies,
        orgInquireVersions,
        orgTagRelease,
        orgUpdateChangeLog,
        releaseStepCommandAndRemaining("+publishSigned"),
        releaseStepCommand("sonatypeBundleRelease"),
        setNextVersion,
        orgCommitNextVersion,
        orgPostRelease
      )
    ) ++ Seq(
      // sbt-org-policies settings:
      // format: OFF
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        FixedCodecovBadge.apply,
        { info => MavenCentralBadge.apply(info.copy(libName = "mu-scala")) },
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
          orgGithubSetting.value.copy(organization = "47deg", project = "mu-scala")
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
      )
    ) ++ macroSettings
  // format: ON
}
