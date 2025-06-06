import sbt._

object AppDependencies {

  private val bootstrapPlayVersion          = "9.12.0"
  private val playHmrcVersion               = "8.0.0"
  private val domainVersion                 = "11.0.0"
  private val reactiveCircuitBreakerVersion = "5.0.0"
  private val emailAddressVersion           = "4.0.0"
  private val flexmarkAllVersion            = "0.36.8"
  private val hmrcMongoVersion              = "2.6.0"

  private val scalaMockVersion = "5.2.0"
  private val pegdownVersion   = "1.6.0"
  private val wiremockVersion  = "2.21.0"
  private val refinedVersion   = "0.11.3"

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30"      % bootstrapPlayVersion,
    "uk.gov.hmrc"                  %% "play-hmrc-api-play-30"          % playHmrcVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"             % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "domain-play-30"                 % domainVersion,
    "uk.gov.hmrc"                  %% "reactive-circuit-breaker"       % reactiveCircuitBreakerVersion,
    "uk.gov.hmrc"                  %% "emailaddress-play-30"           % emailAddressVersion,
    "eu.timepit"                   %% "refined"                        % refinedVersion,
    "com.google.auth"              % "google-auth-library-oauth2-http" % "1.16.0",
    "com.auth0"                    % "java-jwt"                        % "4.4.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"           % "2.14.2",
    "org.mindrot"                  % "jbcrypt"                         % "0.4"
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapPlayVersion % scope,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30" % hmrcMongoVersion     % scope,
    "org.pegdown"          % "pegdown"                  % pegdownVersion       % scope,
    "com.vladsch.flexmark" % "flexmark-all"             % flexmarkAllVersion   % scope
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
            "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
