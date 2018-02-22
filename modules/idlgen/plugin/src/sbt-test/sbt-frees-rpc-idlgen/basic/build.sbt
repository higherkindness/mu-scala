version := "1.0"

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.frees" %% "frees-rpc-server" % "0.11.1"
)
