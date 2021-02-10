import Dependencies._
import ReleaseTransformations._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-consignment-export",
    resolvers ++= Seq[Resolver](
      "TDR Releases" at "s3://tdr-releases-mgmt"
    ),
    libraryDependencies ++= Seq(
      authUtils,
      awsUtils,
      bagit,
      catsEffect,
      decline,
      declineEffect,
      generatedGraphql,
      graphqlClient,
      keycloakCore,
      keycloakAdminClient,
      log4cats,
      log4catsSlf4j,
      mockitoScala % Test,
      mockitoScalaTest % Test,
      pureConfig,
      pureConfigCatsEffect,
      s3Mock,
      scalaCsv,
      scalaTest % Test,
      slf4j
    ),
    packageName in Universal := "tdr-consignment-export",
    fork in Test := true,
    javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf",
    ghreleaseRepoOrg := "nationalarchives",
    ghreleaseAssets := Seq(file((target in Universal).value + (packageName in Universal).value + ".tar.gz")),
    releaseProcess := Seq[ReleaseStep](
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      pushChanges,
      releaseStepInputTask(githubRelease),
//      setNextVersion,
//      commitNextVersion,
//      pushChanges
    )
  ).enablePlugins(JavaAppPackaging, UniversalPlugin)