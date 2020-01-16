import higherkindness.mu.rpc.srcgen.Model.MonixObservable

lazy val root = project
  .in(file("."))
  .settings(
    idlType := "proto",
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_proto",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    srcGenStreamingImplementation := MonixObservable,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-channel" % sys.props("version"),
        "io.higherkindness"    %% "mu-rpc-monix" % sys.props("version")
    )
  )
