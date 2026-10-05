import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "10.8.0"
  private val playHmrcVersion = "9.0.0"
  private val domainVersion = "13.0.0"
  private val reactiveCircuitBreakerVersion = "6.1.0"
  private val flexmarkAllVersion = "0.64.8"
  private val hmrcMongoVersion = "2.14.0"

  private val scalaMockVersion = "7.5.5"
  private val wiremockVersion = "3.13.2"
  private val refinedVersion = "0.11.4"

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30"       % bootstrapPlayVersion,
    "uk.gov.hmrc"                  %% "play-hmrc-api-play-30"           % playHmrcVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"              % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "domain-play-30"                  % domainVersion,
    "uk.gov.hmrc"                  %% "reactive-circuit-breaker"        % reactiveCircuitBreakerVersion,
    "eu.timepit"                   %% "refined"                         % refinedVersion,
    "com.google.auth"               % "google-auth-library-oauth2-http" % "1.54.0",
    "com.auth0"                     % "java-jwt"                        % "4.6.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.22.3.1",
    "org.mindrot"                   % "jbcrypt"                         % "0.4"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30"  % bootstrapPlayVersion % scope,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-30" % hmrcMongoVersion     % scope,
    "com.vladsch.flexmark" % "flexmark-all"            % flexmarkAllVersion   % scope,
    "uk.gov.hmrc"         %% "domain-test-play-30"     % domainVersion        % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test = testCommon(scope) ++ Seq(
          "org.scalamock" %% "scalamock" % scalaMockVersion % scope
        )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope = "it"

        override lazy val test = testCommon(scope) ++ Seq(
          "org.wiremock" % "wiremock" % wiremockVersion % scope
        )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
