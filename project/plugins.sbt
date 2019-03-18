resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.47deg"          % "sbt-org-policies" % "0.11.1")
addSbtPlugin("com.47deg"          % "sbt-microsites"   % "0.9.0")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"    % "0.9.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"          % "0.3.4")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"      % "0.4.0")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"    % "1.6.0-M5")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
