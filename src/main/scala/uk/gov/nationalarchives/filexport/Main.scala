package uk.gov.nationalarchives.filexport

import java.io.File

import cats.effect._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import graphql.codegen.GetFiles.{getFiles => gf}
import graphql.codegen.UpdateExportLocation.{updateExportLocation => uel}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.catnap.syntax.SyntaxForLiftFuture
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import uk.gov.nationalarchives.aws.utils.Clients.s3Async
import uk.gov.nationalarchives.aws.utils.S3Utils
import uk.gov.nationalarchives.filexport.Arguments._
import uk.gov.nationalarchives.filexport.Config.config
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.ExecutionContextExecutor
import scala.language.{implicitConversions, postfixOps}

object Main extends CommandIOApp("tdr-file-export", "Exports tdr files in bagit format", version = "0.0.1") {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def main: Opts[IO[ExitCode]] =
     exportOps.map {
      case FileExport(consignmentId, includeHiddenFiles) => for {
        config <- config()
        tarPath = s"${config.efs.rootLocation}/$consignmentId.tar.gz"
        bashCommands = BashCommands()
        graphQlApi = GraphQlApi(new KeycloakUtils(config.auth.url), new GraphQLClient[gf.Data, gf.Variables](config.api.url), new GraphQLClient[uel.Data, uel.Variables](config.api.url))
        s3Files = S3Files(S3Utils(s3Async))

        data <- graphQlApi.getFiles(config, consignmentId)
        _ <- s3Files.downloadFiles(data, config.s3.cleanBucket, consignmentId, config.efs.rootLocation)
        _ <- Bagit().createBag(consignmentId, config.efs.rootLocation, includeHiddenFiles)
        _ <- bashCommands.runCommand(s"tar -czf $tarPath ${config.efs.rootLocation}/$consignmentId -C ${config.efs.rootLocation}/$consignmentId .")
        _ <- bashCommands.runCommandToFile(s"sha256sum $tarPath", new File(s"$tarPath.sha256"))
        _ <- s3Files.uploadFiles(config.s3.outputBucket, consignmentId, tarPath)
        _ <- graphQlApi.updateExportLocation(config, consignmentId, tarPath)
      } yield ExitCode.Success
    }
}

