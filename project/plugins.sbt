ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml"          % VersionScheme.Always,
  "com.lihaoyi"            %% "geny"               % VersionScheme.Always,
  "org.scala-lang.modules" %% "scala-java8-compat" % VersionScheme.Always
)
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
addSbtPlugin("com.eed3si9n"              % "sbt-projectmatrix"        % "0.11.0")
addSbtPlugin("pl.project13.scala"        % "sbt-jmh"                  % "0.4.7")
addSbtPlugin("com.github.sbt"            % "sbt-ci-release"           % "1.11.1")
addSbtPlugin("com.47deg"                 % "sbt-microsites"           % "1.4.4")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"             % "2.5.4")
addSbtPlugin("org.scalameta"             % "sbt-mdoc"                 % "2.5.2")
addSbtPlugin("de.heikoseeberger"         % "sbt-header"               % "5.10.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github"               % "0.12.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-header"        % "0.12.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-mdoc"          % "0.12.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-remove-test-from-pom" % "0.1.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"             % "0.4.4")
addSbtPlugin("ch.epfl.scala"             % "sbt-missinglink"          % "0.3.6")
addSbtPlugin("io.higherkindness"         % "sbt-mu-srcgen"            % "0.34.0")
