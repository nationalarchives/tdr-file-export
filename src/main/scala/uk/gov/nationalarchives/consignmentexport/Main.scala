package uk.gov.nationalarchives.consignmentexport

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.effect._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import uk.gov.nationalarchives.aws.utils.Clients.{s3Async, sfnAsyncClient}
import uk.gov.nationalarchives.aws.utils.{S3Utils, StepFunctionUtils}
import uk.gov.nationalarchives.consignmentexport.Arguments._
import uk.gov.nationalarchives.consignmentexport.Config.config
import uk.gov.nationalarchives.consignmentexport.StepFunction.ExportOutput

import scala.language.{implicitConversions, postfixOps}

object Main extends CommandIOApp("tdr-consignment-export", "Exports tdr files in bagit format", version = "0.0.1") {
  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def main: Opts[IO[ExitCode]] =
     exportOps.map {
      case FileExport(consignmentId, taskToken) => for {
        config <- config()
        rootLocation = config.efs.rootLocation
        exportId = UUID.randomUUID
        basePath = s"$rootLocation/$exportId"
        tarPath = s"${basePath}/$consignmentId.tar.gz"
        bashCommands = BashCommands()
        graphQlApi = GraphQlApi(config.api.url, config.auth.url)
        keycloakClient = KeycloakClient(config)
        s3Files = S3Files(S3Utils(s3Async))
        stepFunction = StepFunction(StepFunctionUtils(sfnAsyncClient))
        //Export datetime generated as value needed in bag metadata and DB table
        //Cannot use the value from DB table in bag metadata, as bag metadata created before bagging
        //and cannot update DB until bag creation successfully completed
        exportDatetime = ZonedDateTime.now(ZoneOffset.UTC)
        bagMetadata <- BagMetadata(graphQlApi, keycloakClient).getBagMetadata(consignmentId, config, exportDatetime)
        data <- graphQlApi.getFiles(config, consignmentId)
        _ <- IO.fromOption(data.headOption)(new Exception(s"Consignment API returned no files for consignment $consignmentId"))
        _ <- s3Files.downloadFiles(data, config.s3.cleanBucket, consignmentId, basePath)
        _ <- Bagit().createBag(consignmentId, basePath, bagMetadata)
        // The owner and group in the below command have no effect on the file permissions. It just makes tar idempotent
        _ <- bashCommands.runCommand(s"tar --sort=name --owner=root:0 --group=root:0 --mtime ${java.time.LocalDate.now.toString} -C $basePath -c ./$consignmentId | gzip -n > $tarPath")
        _ <- bashCommands.runCommand(s"sha256sum $tarPath > $tarPath.sha256")
        _ <- s3Files.uploadFiles(config.s3.outputBucket, consignmentId, tarPath)
        _ <- graphQlApi.updateExportLocation(config, consignmentId, s"s3://${config.s3.outputBucket}/$consignmentId.tar.gz", exportDatetime)
        _ <- stepFunction.publishSuccess(taskToken, ExportOutput())
      } yield ExitCode.Success
    }
}
