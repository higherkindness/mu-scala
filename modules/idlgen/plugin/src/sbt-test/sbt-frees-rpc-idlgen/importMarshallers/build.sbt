version := "1-SNAPSHOT"

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("version"),
  "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % sys.props("version")
)
