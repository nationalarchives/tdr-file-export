import Dependencies._
import ReleaseTransformations._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "uk.gov.nationalarchives"
ThisBuild / organizationName := "nationalarchives"

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
    mockitoScala % Test,
    mockitoScalaTest % Test,
    pureConfig,
    pureConfigCatsEffect,
    scalaCsv,
    scalaTest % Test,
    slf4j
  ),
  fork in Test := true,
  javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
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
      keycloakCore,
      keycloakAdminClient,
      s3Mock
    ),
    packageName in Universal := "tdr-consignment-export",
    ghreleaseRepoOrg := "nationalarchives",
    ghreleaseAssets := Seq(file((target in Universal).value + (packageName in Universal).value + ".tar.gz")),

  ).enablePlugins(JavaAppPackaging, UniversalPlugin)

releaseProcess := Seq[ReleaseStep](
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  releaseStepInputTask(githubRelease),
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

lazy val authoriser = (project in file("authoriser"))
  .settings(
    commonSettings,
    assemblyJarName in assembly := "consignment-export.jar",
    name := "tdr-consignment-export-authoriser",
  )
