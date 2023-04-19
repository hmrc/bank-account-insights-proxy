import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.14.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % "test, it",
    "org.scalatestplus" % "mockito-4-6_2.13" % "3.2.14.0" % "test, it",
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % "test, it",
  )
}
