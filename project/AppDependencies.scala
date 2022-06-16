import sbt._

object AppDependencies {

  import play.core.PlayVersion

  private val bootstrapPlayVersion          = "5.24.0"
  private val playHmrcVersion               = "7.0.0-play-28"
  private val domainVersion                 = "8.1.0-play-28"
  private val reactiveCircuitBreakerVersion = "3.5.0"
  private val emailAddressVersion           = "3.6.0"
  private val jodaVersion                   = "2.7.4"
  private val flexmarkAllVersion            = "0.36.8"

  private val scalaTestVersion     = "3.2.9"
  private val scalaTestPlusVersion = "5.1.0"
  private val scalaMockVersion     = "4.1.0"
  private val pegdownVersion       = "1.6.0"
  private val wiremockVersion      = "2.21.0"
  private val refinedVersion       = "0.9.4"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"       %% "play-hmrc-api"             % playHmrcVersion,
    "uk.gov.hmrc"       %% "domain"                    % domainVersion,
    "uk.gov.hmrc"       %% "reactive-circuit-breaker"  % reactiveCircuitBreakerVersion,
    "uk.gov.hmrc"       %% "emailaddress"              % emailAddressVersion,
    "eu.timepit"        %% "refined"                   % refinedVersion,
    "com.typesafe.play" %% "play-json-joda"            % jodaVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "org.pegdown"            % "pegdown"             % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current  % scope,
    "org.scalatest"          %% "scalatest"          % scalaTestVersion     % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.vladsch.flexmark"   % "flexmark-all"        % flexmarkAllVersion   % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test = testCommon(scope) ++ Seq(
          "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope,
            "org.scalamock" %% "scalamock"              % scalaMockVersion     % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope = "it"

        override lazy val test = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope
          )
      }.test

    // Transitive dependencies in scalatest/scalatestplusplay drag in a newer version of jetty that is not
    // compatible with wiremock, so we need to pin the jetty stuff to the older version.
    // see https://groups.google.com/forum/#!topic/play-framework/HAIM1ukUCnI
    val jettyVersion = "9.2.13.v20150730"

    def overrides(): Seq[ModuleID] = Seq(
      "org.eclipse.jetty"           % "jetty-server"       % jettyVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyVersion,
      "org.eclipse.jetty"           % "jetty-client"       % jettyVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyVersion
    )
  }

  def apply():     Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
  def overrides(): Seq[ModuleID] = IntegrationTest.overrides()

}
