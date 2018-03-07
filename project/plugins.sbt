resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("io.frees"     % "sbt-freestyle" % "0.3.21")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.8.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
