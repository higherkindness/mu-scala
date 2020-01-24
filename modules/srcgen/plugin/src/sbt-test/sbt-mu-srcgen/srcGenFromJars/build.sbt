lazy val domain = project
  .in(file("domain"))
  .settings(name := "domain")
  .settings(
    Seq(
      organization := "foo.bar",
      organizationName := "Foo Bar",
      scalaVersion := "2.12.4"
    ))
  .settings(version := "1.0.0")
  .settings(Seq(
    publishMavenStyle := true,
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    muSrcGenIdlType := "avro",
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / muSrcGen).taskValue,
    libraryDependencies ++= Seq(
      "io.higherkindness"    %% "mu-rpc-channel" % sys.props("version"),
      "com.chuusai" %% "shapeless" % "2.3.2"
    )
  ))

lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(Seq(
    version := sys.props("version"),
    muSrcGenIdlType := "avro",
    muSrcGenJarNames := Seq("domain"),
    muSrcGenIdlTargetDir := (Compile / sourceManaged).value / "avro",
    muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / muSrcGen).taskValue,
    libraryDependencies ++= Seq(
      "foo.bar" %% "domain" % "1.0.0",
      "io.higherkindness" %% "mu-rpc-server" % sys.props("version")
    )
  ))
