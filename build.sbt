import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion        := 1
ThisBuild / scalaVersion        := "3.3.7"

lazy val microservice = Project("bank-account-insights-proxy", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "buildinfo"
  )
  .settings(PlayKeys.playDefaultPort := 9865)
  .settings(CodeCoverageSettings.settings)

lazy val it = project.in(file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
