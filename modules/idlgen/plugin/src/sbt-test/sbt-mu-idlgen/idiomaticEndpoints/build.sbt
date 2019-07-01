version := sys.props("version")

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("version")
)
