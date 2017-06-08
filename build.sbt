lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .aggregate(httpCore, httpAkka, httpDemo)

lazy val httpCore = (project in file("http/core"))
  .settings(
    libraryDependencies ++= Seq(
      %%("cats-core"),
      %%("shapeless")
    ))

lazy val httpAkka = (project in file("http/akka"))
  .dependsOn(httpCore)
  .settings(
    libraryDependencies ++= Seq(
      %%("akka-http"),
      %%("circe-core"),
      %%("circe-parser")
    ))

lazy val httpDemo = (project in file("http/demo"))
  .dependsOn(httpAkka)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      %%("circe-generic")
    ))
