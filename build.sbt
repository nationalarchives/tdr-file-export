import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

enablePlugins(JavaAppPackaging, UniversalPlugin)

packageName in Universal := "tdr-file-export"

resolvers ++= Seq[Resolver](
  "TDR Releases" at "s3://tdr-releases-mgmt"
)

lazy val root = (project in file("."))
  .settings(
    name := "tdr-file-export",
    libraryDependencies ++= Seq(
      authUtils,
      awsUtils,
      bagit,
      catsEffect,
      decline,
      declineEffect,
      generatedGraphql,
      graphqlClient,
      log4cats,
      log4catsSlf4j,
      mockitoScala % Test,
      mockitoScalaTest % Test,
      pureConfig,
      pureConfigCatsEffect,
      s3Mock,
      slf4j,
      scalaTest % Test
    )
  )

fork in Test := true
javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"
