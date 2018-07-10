resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("io.frees"     % "sbt-freestyle" % "0.3.23")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.8.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
