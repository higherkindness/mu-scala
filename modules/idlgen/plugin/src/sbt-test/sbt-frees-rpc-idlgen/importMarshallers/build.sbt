version := "1-SNAPSHOT"

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")

libraryDependencies ++= Seq(
  "io.frees" %% "frees-rpc-server" % "0.14.2-SNAPSHOT"
)
