import Dependencies._
import sbt.enablePlugins

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val commonSettings = Seq(
  resolvers ++= Seq[Resolver](
    "TDR Releases" at "s3://tdr-releases-mgmt"
  ),
  libraryDependencies ++= Seq(
    catsEffect,
    generatedGraphql,
    graphqlClient,
    log4cats,
    log4catsSlf4j,
    pureConfig,
    pureConfigCatsEffect,
    slf4j
  )
)

lazy val exporter = (project in file("exporter"))
  .settings(
    commonSettings,
    name := "tdr-consignment-export",
    libraryDependencies ++= Seq(
      authUtils,
      awsUtils,
      bagit,
      decline,
      declineEffect,
      mockitoScala % Test,
      mockitoScalaTest % Test,
      s3Mock,
      scalaTest % Test
    ),
    fork in Test := true,
    javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf",
    packageName in Universal := "tdr-consignment-export"
  ).enablePlugins(JavaAppPackaging, UniversalPlugin)

lazy val authoriser = (project in file("authoriser"))
  .settings(
    commonSettings,
    assemblyJarName in assembly := "consignment-export-authoriser.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    name := "tdr-consignment-export-authoriser",
  )




