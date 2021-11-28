ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

lazy val root = (project in file("."))
  .dependsOn(tyrian)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion                    := "3.1.0",
    name                            := "tyrian-http-example",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-parser",
      "io.circe" %%% "circe-generic"
    ).map(_ % "0.14.1"),
    scalafixOnCompile                       := true,
    semanticdbEnabled                       := true,
    semanticdbVersion                       := scalafixSemanticdb.revision
  )

lazy val tyrian = ProjectRef(file("../.."), "tyrian")
