import higherkindness.mu.rpc.srcgen.Model.MonixObservable
import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val root = project
  .in(file("."))
  .settings(
    muSrcGenIdlType := IdlType.Proto,
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_proto",
    muSrcGenStreamingImplementation := MonixObservable,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-channel" % sys.props("version"),
        "io.higherkindness"    %% "mu-rpc-internal-monix" % sys.props("version")
    )
  )
