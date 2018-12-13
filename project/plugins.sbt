resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.47deg"          % "sbt-org-policies" % "0.9.4")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"    % "0.9.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"          % "0.3.4")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
