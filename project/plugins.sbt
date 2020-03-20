resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.47deg"                 % "sbt-org-policies" % "0.13.2")
addSbtPlugin("com.47deg"                 % "sbt-microsites"   % "1.1.4")
addSbtPlugin("com.eed3si9n"              % "sbt-buildinfo"    % "0.9.0")
addSbtPlugin("pl.project13.scala"        % "sbt-jmh"          % "0.3.7")
addSbtPlugin("org.scoverage"             % "sbt-scoverage"    % "1.6.1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"     % "0.1.11")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
