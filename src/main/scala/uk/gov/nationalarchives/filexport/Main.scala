package uk.gov.nationalarchives.filexport

import java.io.{File, FileOutputStream}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}
import java.util.UUID

import cats.effect._
import cats.implicits._
import gov.loc.repository.bagit.creator.BagCreator
import gov.loc.repository.bagit.hash.{StandardSupportedAlgorithms, SupportedAlgorithm}
import gov.loc.repository.bagit.verify.BagVerifier
import gov.loc.repository.bagit.writer.BagWriter
import graphql.codegen.GetFiles.{getFiles => gf}
import graphql.codegen.UpdateExportLocation.{updateExportLocation => uel}
import graphql.codegen.types.UpdateExportLocationInput
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import sttp.client.{SttpBackend, _}
import uk.gov.nationalarchives.aws.utils.Clients.s3Async
import uk.gov.nationalarchives.aws.utils.S3Utils
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.language.{implicitConversions, postfixOps}
import scala.sys.process._
import scala.util.Try

object Main extends IOApp {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  val s3Utils: S3Utils = S3Utils(s3Async)

  case class S3(endpoint: String, cleanBucket: String, outputBucket: String)
  case class Api(url: String)
  case class Auth(url: String, clientId: String, clientSecret: String)
  case class EFS(rootLocation: String)
  case class Configuration(s3: S3, api: Api, auth: Auth, efs: EFS, includeHiddenFiles: Boolean)

  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  implicit class FutureUtils[T](f: Future[T]) {
    def toIO: IO[T] = IO.fromFuture(IO(f))
  }

  implicit class PathUtils(str: String) {
    def toPath: Path = Paths.get(str)
  }

  def commandToFile(output: String, f: File): IO[Unit] =
    Resource.make {
      IO(new FileOutputStream(f))
    } { outStream =>
      IO(outStream.close()).handleErrorWith(_ => IO.unit)
    }.use(fos =>
      IO.pure(fos.write(output.getBytes(Charset.forName("UTF-8")))))


  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- Blocker[IO].use(ConfigSource.default.loadF[IO, Configuration])
      logger <- Slf4jLogger.create[IO]
      getFilesClient = new GraphQLClient[gf.Data, gf.Variables](config.api.url)
      keycloak = new KeycloakUtils(config.auth.url)
      consignmentId <- IO.pure(UUID.fromString(sys.env("CONSIGNMENT_ID")))
      path = s"${config.efs.rootLocation}/$consignmentId"

      _ <- IO.pure(s"mkdir -p $path" !)
      token <- keycloak.serviceAccountToken(config.auth.clientId, config.auth.clientSecret).toIO
      result <- getFilesClient.getResult(token, gf.document, gf.Variables(consignmentId).some).toIO
      data <- IO.fromOption(result.data)(new RuntimeException(s"No files found for consignmnt $consignmentId"))
      _ <- data.getFiles.fileIds.map(fileId => s3Utils.downloadFiles(config.s3.cleanBucket, s"$consignmentId/$fileId")).sequence
      _ <- logger.info("Files downloaded from S3")

      bag <- IO(BagCreator.bagInPlace(consignmentId.toString.toPath, List(StandardSupportedAlgorithms.SHA256: SupportedAlgorithm).asJavaCollection, config.includeHiddenFiles))
      _ <- IO(BagWriter.write(bag, s"$path/output".toPath))
      _ <- IO.fromTry(Try(new BagVerifier().isComplete(bag, config.includeHiddenFiles)))
      _ <- logger.info("Bagit export complete")

      tarPath = s"$path/$consignmentId.tar.gz"
      _ <- IO.pure(s"tar -czf $tarPath $consignmentId/output -C $consignmentId/output" !!)
      _ <- commandToFile(s"sha256sum $tarPath" !!, new File(s"$tarPath.sha256"))
      _ <- logger.info("Tar and sha256sum file created")

      _ <- s3Utils.upload(config.s3.outputBucket, s"$consignmentId.tar.gz", tarPath.toPath)
      _ <- s3Utils.upload(config.s3.outputBucket, s"$consignmentId.tar.gz.sha256", s"$tarPath.sha256".toPath)
      _ <- logger.info("Files uploaded to S3")

      updateExportLocationClient = new GraphQLClient[uel.Data, uel.Variables](config.api.url)
      _ <- updateExportLocationClient.getResult(token, uel.document, uel.Variables(UpdateExportLocationInput(consignmentId, tarPath)).some).toIO
      _ <- logger.info("Export location updated")
    } yield ExitCode.Success
}

