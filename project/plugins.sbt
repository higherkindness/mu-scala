resolvers += Resolver.sonatypeRepo("releases")
addSbtPlugin("io.frees"     % "sbt-freestyle" % "0.3.19")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
