import higherkindness.mu.rpc.srcgen.Model.IdlType

val V = new {
  val circe          = "0.13.0"
  val http4s         = "0.21.0-M6"
  val scalatest      = "3.0.8"
  val logbackClassic = "1.2.3"
  val cats           = "2.0.0"
  val catsEffect     = "2.1.2"
  val fs2            = "2.3.0"
}

lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(version := "1.0.0")
  .settings(Seq(
    muSrcGenIdlType := IdlType.OpenAPI,
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    muSrcGenOpenApiHttpImpl := higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-dsl"    % V.http4s,
      "org.http4s" %% "http4s-circe"  % V.http4s,
      "io.circe"   %% "circe-core"    % V.circe,
      "io.circe"   %% "circe-generic" % V.circe)
    )
  )
  .settings(
    dependencyOverrides ++= overrideDependecies
  )

  lazy val overrideDependecies = Seq(
    "org.typelevel" %% "cats-core"   % V.cats,
    "org.typelevel" %% "cats-effect" % V.catsEffect,
    "co.fs2"        %% "fs2-core"    % V.fs2
  )
