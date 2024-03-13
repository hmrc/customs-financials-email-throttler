import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-29" % "1.7.0",
    "uk.gov.hmrc" %% "bootstrap-backend-play-29" % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-29" % bootstrapVersion % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % "test",
    "org.mockito" % "mockito-core" % "3.1.0" % "test"
  )
}
