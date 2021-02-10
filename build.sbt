import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / organization     := "com.example"
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
      scalaTest % Test,
      slf4j
    ),
    packageName in Universal := "tdr-consignment-export",
    fork in Test := true,
    javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"
  ).enablePlugins(JavaAppPackaging, UniversalPlugin)
