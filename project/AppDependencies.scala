import sbt.*

object AppDependencies {

  val bootstrapVersion = "7.22.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "1.3.0",
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",
    "org.mockito" % "mockito-core" % "3.1.0" % "test"
  )

}
