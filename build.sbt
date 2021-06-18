import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{integrationTestSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-financials-email-throttler"

organization := "uk.gov.hmrc"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(publishingSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    scalaVersion                     := "2.12.11",
    scalacOptions                    := Seq("-feature", "-deprecation"),
    parallelExecution in Test := false,
    fork in Test := false
  )
  .settings(resolvers += Resolver.jcenterRepo)


lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
    ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;" +
    ".*ControllerConfiguration;.*LanguageSwitchController;.*testonly.*;.*views.*;",
  ScoverageKeys.coverageMinimum := 85,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)
