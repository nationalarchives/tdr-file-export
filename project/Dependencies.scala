import sbt._

object Dependencies {
  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.19"
  lazy val awsUtils =  "uk.gov.nationalarchives.aws.utils" %% "tdr-aws-utils" % "0.1.6-SNAPSHOT"
  lazy val bagit = "gov.loc" % "bagit" % "5.2.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.2.0"
  lazy val decline = "com.monovore" %% "decline" % "1.3.0"
  lazy val declineEffect = "com.monovore" %% "decline-effect" % "1.3.0"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.65-SNAPSHOT"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.15"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val log4cats = "io.chrisdavenport" %% "log4cats-core"    % "1.1.1"
  lazy val log4catsSlf4j = "io.chrisdavenport" %% "log4cats-slf4j"   % "1.1.1"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.14.0"
  lazy val pureConfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.14.0"
  lazy val s3Mock = "io.findify" %% "s3mock" % "0.2.6"
  lazy val slf4j = "org.slf4j" % "slf4j-simple" % "1.7.30"
  lazy val mockitoScala = "org.mockito" %% "mockito-scala" % "1.16.0"
  lazy val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % "1.16.0"
}
