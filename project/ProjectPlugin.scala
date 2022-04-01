import microsites.MicrositesPlugin.autoImport._
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls
import mdoc.MdocPlugin.autoImport._
import ch.epfl.scala.sbtmissinglink.MissingLinkPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String                = "4.0.12"
      val betterMonadicFor: String      = "0.3.1"
      val catsEffect: String            = "3.3.9"
      val dockerItScala                 = "0.9.9"
      val dropwizard: String            = "4.2.9"
      val enumeratum: String            = "1.7.0"
      val fs2: String                   = "3.2.5"
      val fs2Grpc: String               = "2.4.7"
      val grpc: String                  = "1.45.1"
      val kindProjector: String         = "0.13.2"
      val log4cats: String              = "2.2.0"
      val log4s: String                 = "1.10.0"
      val logback: String               = "1.2.11"
      val munit: String                 = "0.7.29"
      val munitCE: String               = "1.0.7"
      val natchez: String               = "0.1.6"
      val nettySSL: String              = "2.0.46.Final"
      val paradise: String              = "2.1.1"
      val pbdirect: String              = "0.7.0"
      val prometheus: String            = "0.15.0"
      val pureconfig: String            = "0.17.1"
      val scalaCollectionCompat: String = "2.7.0"
      val scalacheckToolbox: String     = "0.6.0"
      val scalamock: String             = "5.1.0"
      val scalapb: String               = "0.11.10"
      val scalatest: String             = "3.2.11"
      val scalatestplusScheck: String   = "3.2.2.0"
      val slf4j: String                 = "1.7.36"
    }

    lazy val rpcServiceSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel"          %% "cats-effect"             % V.catsEffect,
        "com.thesamet.scalapb"   %% "scalapb-runtime-grpc"    % V.scalapb,
        "org.log4s"              %% "log4s"                   % V.log4s,
        "org.tpolecat"           %% "natchez-core"            % V.natchez,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat,
        "io.grpc"                 % "grpc-stub"               % V.grpc
      ),
      libraryDependencies ++= scalaVersionSpecificDeps(2)(
        "com.sksamuel.avro4s" %% "avro4s-core" % V.avro4s,
        "com.47deg"           %% "pbdirect"    % V.pbdirect,
        "com.beachape"        %% "enumeratum"  % V.enumeratum
      ).value,
      libraryDependencies ++= scalaVersionSpecificDeps(3)(
        "com.sksamuel.avro4s" %% "avro4s-core" % "5.0.0.M1"
      ).value,
      scalacOptions --= on(2, 13)("-Wunused:patvars").value
    )

    lazy val macroSettings: Seq[Setting[_]] = Seq(
      scalacOptions ++= on(2, 13)("-Ymacro-annotations").value
    )

    lazy val fs2Settings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "co.fs2"        %% "fs2-core"         % V.fs2,
        "org.typelevel" %% "fs2-grpc-runtime" % V.fs2Grpc
      )
    )

    lazy val clientNettySettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty" % V.grpc
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

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty" % V.grpc
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
        "io.grpc"        % "grpc-testing" % V.grpc,
        "org.typelevel" %% "cats-effect"  % V.catsEffect
      )
    )

    lazy val nettySslSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.netty" % "netty-tcnative-boringssl-static" % V.nettySSL
      )
    )

    lazy val benchmarksSettings: Seq[Def.Setting[_]] = Seq(
      Compile / unmanagedSourceDirectories += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala"
      },
      Compile / unmanagedResourceDirectories += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "resources"
      },
      Test / unmanagedSourceDirectories += {
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
      },
      libraryDependencies ++= Seq(
        "io.grpc"        % "grpc-all"         % V.grpc,
        "org.slf4j"      % "log4j-over-slf4j" % V.slf4j,
        "org.slf4j"      % "jul-to-slf4j"     % V.slf4j,
        "org.slf4j"      % "jcl-over-slf4j"   % V.slf4j,
        "org.slf4j"      % "slf4j-api"        % V.slf4j,
        "ch.qos.logback" % "logback-core"     % V.logback,
        "ch.qos.logback" % "logback-classic"  % V.logback
      )
    )

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName    := "Mu-Scala",
      micrositeBaseUrl := "mu-scala",
      micrositeDescription := "A purely functional library for building RPC endpoint-based services",
      micrositeGithubOwner          := "higherkindness",
      micrositeGithubRepo           := "mu-scala",
      micrositeGitterChannelUrl     := "47deg/mu",
      micrositeOrganizationHomepage := "https://www.47deg.com",
      micrositePushSiteWith         := GitHub4s,
      mdocIn                        := (Compile / sourceDirectory).value / "docs",
      micrositeGithubToken          := Option(System.getenv().get("GITHUB_TOKEN")),
      micrositePalette := Map(
        "brand-primary"   -> "#001e38",
        "brand-secondary" -> "#F44336",
        "white-color"     -> "#E6E7EC"
      ),
      micrositeHighlightTheme := "github-gist",
      micrositeHighlightLanguages += "protobuf"
    )

    lazy val docsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.scalatest"        %% "scalatest"       % V.scalatest,
        "org.scalatestplus"    %% "scalacheck-1-14" % V.scalatestplusScheck,
        "io.dropwizard.metrics" % "metrics-jmx"     % V.dropwizard,
        "org.tpolecat"         %% "natchez-jaeger"  % V.natchez
      ),
      scalacOptions ~= (_ filterNot Set(
        "-Xfatal-warnings",
        "-Ywarn-unused-import",
        "-Xlint"
      ).contains)
    )

    lazy val testSettings = Seq(
      publishArtifact          := false,
      Test / parallelExecution := false,
      testFrameworks += new TestFramework("munit.Framework"),
      libraryDependencies ++= Seq(
        "io.grpc"    % "grpc-netty"                      % V.grpc              % Test,
        "io.netty"   % "netty-tcnative-boringssl-static" % V.nettySSL          % Test,
        "com.47deg" %% "scalacheck-toolbox-datetime"     % V.scalacheckToolbox % Test,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat % Test,
        "org.scalameta"          %% "munit"                   % V.munit                 % Test,
        "org.scalameta"          %% "munit-scalacheck"        % V.munit                 % Test,
        "org.typelevel"          %% "munit-cats-effect-3"     % V.munitCE               % Test,
        "ch.qos.logback"          % "logback-classic"         % V.logback               % Test,
        "org.slf4j"               % "slf4j-nop"               % V.slf4j                 % Test
      )
    )

    lazy val haskellIntegrationTestSettings = Seq(
      publishArtifact          := false,
      Test / parallelExecution := false,
      libraryDependencies ++= Seq(
        "co.fs2"        %% "fs2-core"                    % V.fs2,
        "org.scalameta" %% "munit"                       % V.munit         % Test,
        "org.typelevel" %% "munit-cats-effect-3"         % V.munitCE       % Test,
        "com.whisk"     %% "docker-testkit-impl-spotify" % V.dockerItScala % Test
      )
    )

    def on[A](major: Int, minor: Int)(a: A): Def.Initialize[Seq[A]] =
      Def.setting {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some(v) if v == (major, minor) => Seq(a)
          case _                              => Nil
        }
      }

    def scalaVersionSpecificDeps(
        majorScalaVersion: Int
    )(moduleIDs: ModuleID*): Def.Initialize[Seq[ModuleID]] =
      Def.setting {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((v, _)) if v == majorScalaVersion => moduleIDs
          case _                                      => Nil
        }
      }

  }

  lazy val missingLinkSettings: Seq[Def.Setting[_]] =
    Seq(
      missinglinkExcludedDependencies += moduleFilter(organization = "ch.qos.logback"),
      missinglinkExcludedDependencies += moduleFilter(
        organization = "org.slf4j",
        name = "slf4j-api"
      ),
      missinglinkExcludedDependencies += moduleFilter(
        organization = "org.apache.commons",
        name = "commons-compress"
      ),
      missinglinkExcludedDependencies += moduleFilter(
        organization = "io.prometheus",
        name = "simpleclient_tracer_otel"
      ),
      missinglinkExcludedDependencies += moduleFilter(
        organization = "io.prometheus",
        name = "simpleclient_tracer_otel_agent"
      ),
      missinglinkIgnoreSourcePackages += IgnoredPackage("org.apache.avro.file"),
      missinglinkIgnoreSourcePackages += IgnoredPackage("io.netty.util.internal.logging"),
      missinglinkIgnoreSourcePackages += IgnoredPackage("io.netty.handler.ssl")
    )

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      Test / fork            := false,
      Compile / compileOrder := CompileOrder.JavaThenScala,
      coverageFailOnMinimum  := false,
      libraryDependencies ++= scalaVersionSpecificDeps(2)(
        compilerPlugin(
          "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
        )
      ).value
    ) ++ macroSettings ++ missingLinkSettings
}
