import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val root = project
  .in(file("."))
  .settings(
    muSrcGenIdlType := IdlType.Proto,
    muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_proto",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-channel" % sys.props("version"),
        "io.higherkindness"    %% "mu-rpc-fs2" % sys.props("version")
    )
  )
