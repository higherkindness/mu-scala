version := sys.props("version")

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-channel" % sys.props("version")
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
