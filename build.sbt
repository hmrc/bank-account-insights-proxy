import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion        := "2.13.12"
ThisBuild / majorVersion        := 0

lazy val microservice = Project("bank-account-insights-proxy", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "buildinfo"
  )
  .settings(PlayKeys.playDefaultPort := 9865)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)

lazy val it = project.in(file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings)
  .settings(
    libraryDependencies ++= AppDependencies.itTest
  )
