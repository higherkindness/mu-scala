resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("io.frees"           % "sbt-freestyle" % "0.3.23")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo" % "0.8.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"       % "0.3.4")
addSbtPlugin("com.47deg"    % "sbt-microsites" % "0.7.23")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
