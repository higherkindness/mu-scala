version := "1-SNAPSHOT"

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.higherkindness" %% "frees-rpc-server" % sys.props("version"),
  "io.higherkindness" %% "frees-rpc-marshallers-jodatime" % sys.props("version")
)
