version := "1-SNAPSHOT"

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.frees" %% "frees-rpc-server" % sys.props("version"),
  "io.frees" %% "frees-rpc-marshallers-jodatime" % sys.props("version")
)
