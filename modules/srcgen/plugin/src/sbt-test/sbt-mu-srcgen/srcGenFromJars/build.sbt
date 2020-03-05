import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val domain = project
  .in(file("domain"))
  .settings(name := "domain")
  .settings(
    Seq(
      organization := "foo.bar",
      scalaVersion := "2.12.10"
    ))
  .settings(version := "1.0.0")
  .settings(Seq(
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    muSrcGenIdlType := IdlType.Avro,
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-channel" % sys.props("version")
    )
  ))

lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(Seq(
    version := sys.props("version"),
    muSrcGenIdlType := IdlType.Avro,
    muSrcGenJarNames := Seq("domain"),
    muSrcGenIdlTargetDir := (Compile / sourceManaged).value / "avro",
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "foo.bar" %% "domain" % "1.0.0"
    )
  ))
