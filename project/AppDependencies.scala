import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "8.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % Test,
    "org.scalatestplus" % "mockito-4-6_2.13" % "3.2.15.0" % "test",
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.6" % "test",
  )

  val itTest = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % "it",
    "org.scalatestplus" % "mockito-4-6_2.13" % "3.2.15.0" % "it",
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.6" % "it",
  )
}
