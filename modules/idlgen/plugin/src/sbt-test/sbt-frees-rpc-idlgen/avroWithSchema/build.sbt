version := sys.props("version")

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("version")
)
