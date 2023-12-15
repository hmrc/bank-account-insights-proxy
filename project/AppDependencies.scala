import sbt.*

object AppDependencies {
  private val bootstrapPlayVersion = "8.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion
  )

  val test: Seq[ModuleID] = Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % Test)
}
