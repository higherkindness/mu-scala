import microsites.MicrositesPlugin.autoImport._
import sbt.Keys._
import sbt._
import com.alejandrohdezma.sbt.github.SbtGithubPlugin
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls
import mdoc.MdocPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = SbtGithubPlugin

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
      val grpc: String                  = "1.29.0"
      val jodaTime: String              = "2.10.6"
      val http4s: String                = "0.21.0-M6"
      val kindProjector: String         = "0.10.3"
      val lastRelease                   = "0.21.3"
      val log4cats: String              = "1.0.1"
      val log4s: String                 = "1.8.2"
      val logback: String               = "1.2.3"
      val monix: String                 = "3.2.0"
      val natchez: String               = "0.0.11"
      val nettySSL: String              = "2.0.30.Final"
      val paradise: String              = "2.1.1"
      val pbdirect: String              = "0.5.1"
      val prometheus: String            = "0.8.1"
      val pureconfig: String            = "0.12.3"
      val reactiveStreams: String       = "1.0.3"
      val scala212: String              = "2.12.11"
      val scala213: String              = "2.13.2"
      val scalaCollectionCompat: String = "2.1.6"
      val scalacheck: String            = "1.14.3"
      val scalacheckToolbox: String     = "0.3.5"
      val scalamock: String             = "4.4.0"
      val scalatest: String             = "3.1.1"
      val scalatestplusScheck: String   = "3.1.1.1"
      val slf4j: String                 = "1.7.30"
    }

    lazy val noPublishSettings = Seq(
      publish := ((): Unit),
      publishArtifact := false,
      publishMavenStyle := false // suppress warnings about intransitive deps (not published anyway)
    )

    lazy val rpcServiceSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "org.typelevel"          %% "cats-effect"                 % V.catsEffect,
        "com.47deg"              %% "pbdirect"                    % V.pbdirect,
        "com.beachape"           %% "enumeratum"                  % V.enumeratum,
        "com.sksamuel.avro4s"    %% "avro4s-core"                 % V.avro4s,
        "org.log4s"              %% "log4s"                       % V.log4s,
        "org.tpolecat"           %% "natchez-core"                % V.natchez,
        "org.scala-lang.modules" %% "scala-collection-compat"     % V.scalaCollectionCompat,
        "io.grpc"                % "grpc-stub"                    % V.grpc,
        "io.grpc"                % "grpc-netty"                   % V.grpc % Test,
        "org.scalamock"          %% "scalamock"                   % V.scalamock % Test,
        "com.47deg"              %% "scalacheck-toolbox-datetime" % V.scalacheckToolbox % Test,
        "org.scalatestplus"      %% "scalacheck-1-14"             % V.scalatestplusScheck % Test
      ),
      // Disable this flag because quasiquotes trigger a lot of false positive warnings
      scalacOptions -= "-Wunused:patvars", // for Scala 2.13
      scalacOptions -= "-Ywarn-unused:params" // for Scala 2.12
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

    lazy val monixSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.monix"            %% "monix"           % V.monix,
        "org.reactivestreams" % "reactive-streams" % V.reactiveStreams
      )
    )

    lazy val fs2Settings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "co.fs2"                %% "fs2-core"     % V.fs2,
        "org.lyranthe.fs2-grpc" %% "java-runtime" % V.fs2Grpc
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
        "io.grpc"                % "grpc-testing"             % V.grpc,
        "org.typelevel"          %% "cats-effect"             % V.catsEffect,
        "org.scalacheck"         %% "scalacheck"              % V.scalacheck % Test,
        "org.scalatestplus"      %% "scalacheck-1-14"         % V.scalatestplusScheck % Test,
        "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat % Test
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

    lazy val benchmarksSettings: Seq[Def.Setting[_]] = Seq(
      unmanagedSourceDirectories in Compile += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala"
      },
      unmanagedResourceDirectories in Compile += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "resources"
      },
      unmanagedSourceDirectories in Test += {
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
      },
      libraryDependencies ++= Seq(
        "io.grpc"           % "grpc-all"         % V.grpc,
        "io.chrisdavenport" %% "log4cats-core"   % V.log4cats,
        "io.chrisdavenport" %% "log4cats-slf4j"  % V.log4cats,
        "org.slf4j"         % "log4j-over-slf4j" % V.slf4j,
        "org.slf4j"         % "jul-to-slf4j"     % V.slf4j,
        "org.slf4j"         % "jcl-over-slf4j"   % V.slf4j,
        "org.slf4j"         % "slf4j-api"        % V.slf4j,
        "ch.qos.logback"    % "logback-core"     % V.logback,
        "ch.qos.logback"    % "logback-classic"  % V.logback
      )
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
      micrositeGithubToken := Option(System.getenv().get("GITHUB_TOKEN")),
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
        "org.scalatest"         %% "scalatest"       % V.scalatest,
        "org.scalatestplus"     %% "scalacheck-1-14" % V.scalatestplusScheck,
        "io.dropwizard.metrics" % "metrics-jmx"      % V.dropwizard,
        "org.tpolecat"          %% "natchez-jaeger"  % V.natchez
      )
    ) ++ mdocSettings

    lazy val haskellIntegrationTestSettings = Seq(
      publishArtifact := false,
      Test / parallelExecution := false,
      libraryDependencies ++= Seq(
        "co.fs2"        %% "fs2-core"                    % V.fs2,
        "org.scalatest" %% "scalatest"                   % V.scalatest % Test,
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

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
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
      )
    ) ++ macroSettings
}
