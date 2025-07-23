import microsites.MicrositesPlugin.autoImport._
import sbt.Keys._
import sbt._

import scala.language.reflectiveCalls
import mdoc.MdocPlugin.autoImport._
import ch.epfl.scala.sbtmissinglink.MissingLinkPlugin.autoImport._
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.SrcGenPlugin.autoImport._
import _root_.io.github.davidgregory084.ScalacOptions
import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport._

import scala.language.reflectiveCalls

object ProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String                = "4.1.2"
      val catsEffect: String            = "3.6.3"
      val catsRetry: String             = "3.1.3"
      val dockerItScala                 = "0.12.0"
      val dropwizard: String            = "4.2.33"
      val enumeratum: String            = "1.9.0"
      val fs2: String                   = "3.12.0"
      val fs2Grpc: String               = "2.8.2"
      val grpc: String                  = "1.73.0"
      val kindProjector: String         = "0.13.3"
      val log4cats: String              = "2.7.1"
      val log4s: String                 = "1.10.0"
      val logback: String               = "1.5.18"
      val munit: String                 = "1.1.0"
      val munitSC: String               = "1.1.0"
      val munitCE: String               = "2.1.0"
      val natchez: String               = "0.3.8"
      val nettySSL: String              = "2.0.65.Final"
      val paradise: String              = "2.1.1"
      val pbdirect: String              = "0.7.0"
      val prometheus: String            = "0.16.0"
      val pureconfig: String            = "0.17.9"
      val scalaCollectionCompat: String = "2.13.0"
      val scalacheckToolbox: String     = "0.7.0"
      val scalamock: String             = "5.1.0"
      val scalapb: String               = "0.11.19"
      val scalatest: String             = "3.2.12"
      val scalatestplusScheck: String   = "3.2.2.0"
      val slf4j: String                 = "2.0.17"
    }

    lazy val rpcServiceSettings: Seq[Def.Setting[_]] = Seq(
      testFrameworks += new TestFramework("munit.Framework"),
      libraryDependencies ++= Seq(
        "org.typelevel"          %% "cats-effect"             % V.catsEffect,
        "com.thesamet.scalapb"   %% "scalapb-runtime-grpc"    % V.scalapb,
        "org.log4s"              %% "log4s"                   % V.log4s,
        "org.tpolecat"           %% "natchez-core"            % V.natchez,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat,
        "io.grpc"                 % "grpc-stub"               % V.grpc,
        "org.scalameta"          %% "munit"                   % V.munit % Test
      ),
      libraryDependencies ++= scalaVersionSpecificDeps(2)(
        "com.sksamuel.avro4s" %% "avro4s-core" % V.avro4s,
        "com.47deg"           %% "pbdirect"    % V.pbdirect,
        "com.beachape"        %% "enumeratum"  % V.enumeratum
      ).value,
      libraryDependencies ++= scalaVersionSpecificDeps(3)(
        "com.sksamuel.avro4s" %% "avro4s-core" % "5.0.14"
      ).value,
      scalacOptions --= on(2, 13)("-Wunused:patvars").value,
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
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
      ),
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
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
        "org.typelevel" %% "cats-effect" % V.catsEffect
      )
    )

    lazy val healthCheckSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect" % V.catsEffect
      ),
      muSrcGenIdlType := IdlType.Proto,
      scalacOptions += "-Wconf:src=src_managed/.*:silent",
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
    )

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty" % V.grpc
      ),
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
    )

    lazy val configSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "com.github.pureconfig" %% "pureconfig"          % V.pureconfig,
        "org.scalameta"         %% "munit"               % V.munit      % Test,
        "org.scalameta"         %% "munit-scalacheck"    % V.munitSC    % Test,
        "org.typelevel"         %% "munit-cats-effect"   % V.munitCE    % Test,
        "org.typelevel"         %% "cats-effect-testkit" % V.catsEffect % Test
      )
    )

    lazy val prometheusMetricsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.prometheus" % "simpleclient" % V.prometheus
      ),
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
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
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala",
      Compile / unmanagedResourceDirectories +=
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "resources",
      Test / unmanagedSourceDirectories +=
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala",
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
      mdocExtraArguments            := Seq("--no-link-hygiene"),
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
        "org.scalameta"        %% "munit-scalacheck"  % V.munitSC,
        "org.typelevel"        %% "munit-cats-effect" % V.munitCE,
        "io.dropwizard.metrics" % "metrics-jmx"       % V.dropwizard,
        "org.tpolecat"         %% "natchez-jaeger"    % V.natchez
      ),
      scalacOptions ~= (_ filterNot Set(
        "-Xfatal-warnings",
        "-Ywarn-unused-import",
        "-Xlint"
      ).contains)
    )

    lazy val testUtilsSettings = Seq(
      publishArtifact          := false,
      Test / parallelExecution := false,
      testFrameworks += new TestFramework("munit.Framework"),
      scalacOptions -= "-Xfatal-warnings",
      libraryDependencies ++= Seq(
        "io.grpc"        % "grpc-core"        % V.grpc,
        "org.scalameta" %% "munit-scalacheck" % V.munitSC,
        "org.typelevel" %% "cats-effect"      % V.catsEffect
      )
    )
    lazy val testSettings = Seq(
      publishArtifact          := false,
      Test / parallelExecution := false,
      testFrameworks += new TestFramework("munit.Framework"),
      scalacOptions -= "-Xfatal-warnings",
      libraryDependencies ++= Seq(
        "io.grpc"    % "grpc-netty"                      % V.grpc              % Test,
        "io.netty"   % "netty-tcnative-boringssl-static" % V.nettySSL          % Test,
        "com.47deg" %% "scalacheck-toolbox-datetime"     % V.scalacheckToolbox % Test,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat % Test,
        "org.scalameta"          %% "munit"                   % V.munit                 % Test,
        "org.scalameta"          %% "munit-scalacheck"        % V.munitSC               % Test,
        "org.typelevel"          %% "munit-cats-effect"       % V.munitCE               % Test,
        "org.typelevel"          %% "cats-effect-testkit"     % V.catsEffect            % Test,
        "ch.qos.logback"          % "logback-classic"         % V.logback               % Test,
        "com.github.cb372"       %% "cats-retry"              % V.catsRetry             % Test
      )
    )

    lazy val protobufSrcGenSettings = Seq(
      muSrcGenIdlType := IdlType.Proto,
      scalacOptions += "-Wconf:src=src_managed/.*:silent",
      scalacOptions --= on(3, 4)("-Xfatal-warnings").value
    )

    lazy val protobufRPCTestSettings = testSettings ++ protobufSrcGenSettings

    lazy val avroSrcGenSettings = Seq(
      muSrcGenIdlType           := IdlType.Avro,
      muSrcGenSerializationType := SerializationType.Avro,
      tpolecatScalacOptions ~= { options =>
        // sbt-mu-srcgen generates the 'bigDecimalTagged' import, which can be unused
        options.filterNot(
          Set(
            ScalacOptions.privateWarnUnusedImport,
            ScalacOptions.privateWarnUnusedImports,
            ScalacOptions.warnUnusedImports
          )
        )
      },
      scalacOptions += "-Wconf:src=src_managed/.*:silent"
    )

    lazy val avroRPCTestSettings = testSettings ++ avroSrcGenSettings

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

  import autoImport.*

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      Test / fork            := false,
      Compile / compileOrder := CompileOrder.JavaThenScala,
      libraryDependencies ++= scalaVersionSpecificDeps(2)(
        compilerPlugin(
          "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
        )
      ).value
    ) ++ macroSettings ++ missingLinkSettings
}
