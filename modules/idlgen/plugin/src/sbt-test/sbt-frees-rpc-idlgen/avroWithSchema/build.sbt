version := sys.props("version")

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.frees" %% "frees-rpc-server" % sys.props("version")
)
