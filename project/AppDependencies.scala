import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "simple-reactivemongo"      % "7.31.0-play-27",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27" % "3.4.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "3.0.0"                 % Test,
    "org.scalatest"           %% "scalatest"                % "3.0.7"                 % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"                 % "test",
    "org.mockito"             %  "mockito-core"             % "3.1.0"                % "test"
  )

}
