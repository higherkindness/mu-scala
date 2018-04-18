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
    srcGenSourceDir := (Compile / resourceDirectory).value,
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (srcGen in Compile).taskValue,
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2"
    )
  ))

lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(Seq(
    version := sys.props("version"),
    resolvers += Resolver.bintrayRepo("beyondthelines", "maven"),
    idlType := "avro",
    srcJarNames := Seq("domain"),
    srcGenIDLTargetDir := (Compile / sourceManaged).value / "avro",
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (srcGenFromJars in Compile).taskValue,
    libraryDependencies ++= Seq(
      "foo.bar" %% "domain" % "1.0.0",
      "io.frees" %% "frees-rpc-server" % sys.props("version")
    )
  ))
