lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(version := "1.0.0")
  .settings(Seq(
    publishMavenStyle := true,
    mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".class")) },
    idlType := "proto",
    srcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_proto",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-channel" % sys.props("version"),
        "io.higherkindness"    %% "mu-rpc-fs2" % sys.props("version"),
        "com.chuusai" %% "shapeless" % "2.3.2"
    )
  ))
