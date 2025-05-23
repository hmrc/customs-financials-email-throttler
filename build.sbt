import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "customs-financials-email-throttler"

val scalaStyleConfigFile     = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"
val testDirectory            = "test"

val scala3_3_5      = "3.3.5"
val silencerVersion = "1.7.14"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala3_3_5

organization := "uk.gov.hmrc"

lazy val scalastyleSettings = Seq(
  scalastyleConfig := baseDirectory.value / "scalastyle-config.xml",
  (Test / scalastyleConfig) := baseDirectory.value / testDirectory / "test-scalastyle-config.xml"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scoverageSettings *)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),
    scalacOptions += "-Wconf:msg=Flag.*repeatedly:s",
    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"
    ),
    libraryDependencies ++= Seq(
      compilerPlugin(
        "com.github.ghik" % "silencer-plugin" % silencerVersion
          cross CrossVersion.for3Use2_13With("", ".12")
      ),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided
        cross CrossVersion.for3Use2_13With("", ".12")
    ),
    scalacOptions := Seq("-feature", "-deprecation"),
    Test / parallelExecution := false,
    Test / fork := false,
    scalafmtDetailedError := true,
    scalafmtPrintDiff := true,
    scalafmtFailOnErrors := true
  )
  .settings(scalastyleSettings)

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*BuildInfo.*;.*javascript.*;.*Routes.*;" +
    ".*GuiceInjector;.*testonly.*;.*EmailQueue;",
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageMinimumBranchTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := false,
  ScoverageKeys.coverageHighlighting := true
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % AppDependencies.bootstrapVersion % Test)
  )

addCommandAlias(
  "runAllChecks",
  ";clean;compile;coverage;test;it/test;scalafmtCheckAll;scalastyle;Test/scalastyle;coverageReport"
)
