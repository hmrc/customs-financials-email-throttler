import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"        % "0.56.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28" % "5.16.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.16.0"                 % Test,
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.0.0"                 % "test",
    "org.mockito"             %  "mockito-core"             % "3.1.0"                % "test"
  )

}
