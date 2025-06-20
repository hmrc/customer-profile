import play.sbt.PlayImport.PlayKeys.playDefaultPort

val appName: String = "customer-profile"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin, ScoverageSbtPlugin): _*
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(scalafmtOnCompile := true)
  .settings(
    Seq(
      routesImport ++= Seq(
        "uk.gov.hmrc.domain._",
        "uk.gov.hmrc.customerprofile.binder.Binders._",
        "uk.gov.hmrc.customerprofile.domain.types._",
        "uk.gov.hmrc.customerprofile.domain.types.JourneyId._"
      )
    )
  )
  .settings(
    majorVersion := 1,
    scalaVersion := "3.6.4",
    playDefaultPort := 8233,
    libraryDependencies ++= AppDependencies(),
    update / evictionWarningOptions := EvictionWarningOptions.default
      .withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    IntegrationTest / unmanagedResourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it-resources")).value,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-numeric-widen",
      "-Xlint"
    ),
    coverageMinimumStmtTotal := 83,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;.*Routes.*;app.*;.*prod;.*definition;.*testOnlyDoNotUseInAppConf;.*com.kenshoo.*;.*javascript.*;.*BuildInfo;.*Reverse.*;.*Binders.*;.*testOnly.*;.*api.*;"
  )
