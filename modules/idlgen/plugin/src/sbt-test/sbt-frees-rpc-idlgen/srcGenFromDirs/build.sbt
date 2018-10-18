lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(version := "1.0.0")
  .settings(Seq(
    publishMavenStyle := true,
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    idlType := "avro",
    srcGenSourceDirs := Seq(
      (Compile / resourceDirectory).value / "domain",
      (Compile / resourceDirectory).value / "protocol"),
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    libraryDependencies ++= Seq(
      "io.higherkindness"    %% "mu-rpc-client-core" % sys.props("version"),
      "com.chuusai" %% "shapeless"        % "2.3.2"
    )
  ))
