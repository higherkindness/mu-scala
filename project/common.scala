import sbt.Keys._
import sbt._

import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPattern
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderKey.headers
import com.typesafe.sbt.SbtPgp.autoImport._
//import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import scala.{ Console => C }

object BuildCommon extends AutoPlugin {

  override def requires = plugins.JvmPlugin && HeaderPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val noPublishSettings = Seq(
      publish := (),
      publishLocal := (),
      publishArtifact := false)
  }

  override def projectSettings =
    baseSettings ++
    enhancingScalaSettings ++
    publishSettings ++
    AutomateHeaderPlugin.projectSettings

  private[this] def baseSettings = Seq(
    scalaVersion := "2.12.1",
    scalaOrganization := "org.typelevel",
    crossScalaVersions := Seq("2.11.8", "2.12.0"),

    organization := "xyz.anamorph",
    description := "ADT driven clients and adapters",

    fork in run := true,
    fork in Test := true,
    //fork in Test := !isScalaJSProject.value,
    parallelExecution in Test := false,
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    cancelable in Global := true,

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      //"-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused-import",
      "-Xfuture",
      "-Yno-predef",
      "-Ypartial-unification"),

    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Nil
      case Some((2, 12)) => Seq("-Yliteral-types")
      case _             => Nil
    }),

    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq(
        compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
      case Some((2, 12)) => Nil
      case _             => Nil
    }),

    scalacOptions in (Compile, doc) :=
      (scalacOptions in (Compile, doc)).value.filter(_ != "-Xfatal-warnings"),

    headers := Map(
      "scala" → (
        HeaderPattern.cStyleBlockComment,
        s"""|/* -
         | * Mezzo [${name.value}]
         | */
         |
         |""".stripMargin))
  )

  private[this] def enhancingScalaSettings = Seq(
    //resolvers += Resolver.bintrayRepo("tek", "maven"),
    //libraryDependencies ++= Seq(
      //compilerPlugin("tryp" %% "splain" % "0.1.11")),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq(
      compilerPlugin(
        "org.spire-math" %% "kind-projector" % "0.9.3" cross CrossVersion.binary)
    )
  )

  private[this] lazy val gpgFolder = sys.env.getOrElse("GPG_FOLDER", ".")

  private[this] lazy val publishSettings = Seq(
    pgpPassphrase := Some(sys.env.getOrElse("GPG_PASSPHRASE", "").toCharArray),
    pgpPublicRing := file(s"$gpgFolder/pubring.gpg"),
    pgpSecretRing := file(s"$gpgFolder/secring.gpg"),
    credentials += Credentials("Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      sys.env.getOrElse("PUBLISH_USERNAME", ""),
      sys.env.getOrElse("PUBLISH_PASSWORD", "")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/andyscott/mezzo"),
      "https://github.com/andyscott/mezzo.git")),
    startYear := Some(2016),
    homepage := Option(url("https://github.com/andyscott/mezzo")),
    organizationHomepage := Option(new URL("http://mezzo.anamorph.xyz")),
    licenses := Seq("Apache License, Version 2.0" ->
      url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := Function.const(false),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("Snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra :=
      <developers>
        <developer>
          <name>Andy Scott</name>
          <email>andy.g.scott@gmail.com</email>
        </developer>
      </developers>
  )

}
