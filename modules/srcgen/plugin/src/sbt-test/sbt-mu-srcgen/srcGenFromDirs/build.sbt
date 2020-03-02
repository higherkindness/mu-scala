import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val root = project
  .in(file("."))
  .settings(name := "root")
  .settings(version := "1.0.0")
  .settings(Seq(
    muSrcGenIdlType := IdlType.Avro,
    muSrcGenSourceDirs := Seq(
      (Compile / resourceDirectory).value / "domain",
      (Compile / resourceDirectory).value / "protocol"
    ),
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-channel" % sys.props("version")
    )
  ))
