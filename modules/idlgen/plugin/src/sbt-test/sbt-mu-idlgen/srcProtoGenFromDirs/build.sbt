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
    libraryDependencies ++= Seq(
      "io.higherkindness"    %% "mu-rpc-channel" % "0.17.3-SNAPSHOT",
      "com.chuusai" %% "shapeless"        % "2.3.2"
    )
  ))
