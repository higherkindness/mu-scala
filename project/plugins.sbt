ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "com.lihaoyi"            %% "geny"      % VersionScheme.Always
)
addSbtPlugin("com.eed3si9n"              % "sbt-projectmatrix"        % "0.9.0")
addSbtPlugin("pl.project13.scala"        % "sbt-jmh"                  % "0.4.5")
addSbtPlugin("com.github.sbt"            % "sbt-ci-release"           % "1.5.12")
addSbtPlugin("com.47deg"                 % "sbt-microsites"           % "1.4.3")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"             % "2.5.0")
addSbtPlugin("org.scalameta"             % "sbt-mdoc"                 % "2.3.7")
addSbtPlugin("de.heikoseeberger"         % "sbt-header"               % "5.10.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github"               % "0.11.8")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-header"        % "0.11.8")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-mdoc"          % "0.11.8")
addSbtPlugin("com.alejandrohdezma"       % "sbt-remove-test-from-pom" % "0.1.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"             % "0.4.2")
addSbtPlugin("ch.epfl.scala"             % "sbt-missinglink"          % "0.3.5")
addSbtPlugin("io.higherkindness"         % "sbt-mu-srcgen"            % "0.30.3")
