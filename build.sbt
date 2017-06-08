lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .aggregate(httpCore, httpAkka, httpDemo)

lazy val httpCore = (project in file("http/core"))
  .settings(crossVersionSharedSources)
  .settings(libraryDependencies ++= Seq(
    %%("cats-core"),
    %%("shapeless")
  ))

lazy val httpAkka = (project in file("http/akka"))
  .dependsOn(httpCore)
  .settings(libraryDependencies ++= Seq(
    %%("akka-http"),
      %%("circe-core"),
        %%("circe-parser")
  ))

lazy val httpDemo =(project in file("http/demo"))
  .dependsOn(httpAkka)
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= Seq(
      %%("circe-generic")
  ))


lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc ).value.flatMap { dir: File =>
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, y)) if y == 11 => Some(new File(dir.getPath + "_2.11"))
          case _                       => None
        }
      }
    }
  }
