version := "1-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("version"),
  "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % sys.props("version")
)
