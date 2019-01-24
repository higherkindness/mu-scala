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
    idlType := "avro",
    srcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
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
    idlType := "avro",
    srcGenJarNames := Seq("domain"),
    srcGenIDLTargetDir := (Compile / sourceManaged).value / "avro",
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    libraryDependencies ++= Seq(
      "foo.bar" %% "domain" % "1.0.0",
      "io.higherkindness" %% "mu-rpc-server" % sys.props("version")
    )
  ))
