package uk.gov.nationalarchives.consignmentexport

import cats.effect._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import uk.gov.nationalarchives.aws.utils.Clients.s3Async
import uk.gov.nationalarchives.aws.utils.S3Utils
import uk.gov.nationalarchives.consignmentexport.Arguments._
import uk.gov.nationalarchives.consignmentexport.Config.config

import scala.language.{implicitConversions, postfixOps}

object Main extends CommandIOApp("tdr-consignment-export", "Exports tdr files in bagit format", version = "0.0.1") {
  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def main: Opts[IO[ExitCode]] =
     exportOps.map {
      case FileExport(consignmentId) => for {
        config <- config()
        tarPath = s"${config.efs.rootLocation}/$consignmentId.tar.gz"
        bashCommands = BashCommands()
        graphQlApi = GraphQlApi(config.api.url, config.auth.url)
        keycloakClient = KeycloakClient(config)
        s3Files = S3Files(S3Utils(s3Async))
        bagit = Bagit()
        validator = Validator(consignmentId)
        consignmentResult <- graphQlApi.getConsignmentMetadata(config, consignmentId)
        consignmentData <- validator.validateConsignmentResult(consignmentResult)
        _ <- validator.validateConsignmentHasFiles(consignmentData)
        bagMetadata <- BagMetadata(keycloakClient, config).generateMetadata(consignmentId, consignmentData)
        validatedFileMetadata <- validator.validateFileMetadataNotEmpty(consignmentData.files)
        _ <- s3Files.downloadFiles(validatedFileMetadata, config.s3.cleanBucket, consignmentId, config.efs.rootLocation)
        bag <- bagit.createBag(consignmentId, config.efs.rootLocation, bagMetadata)
        fileMetadataCsv <- BagAdditionalFiles(bag.getRootDir).fileMetadataCsv(validatedFileMetadata)
        checksums <- ChecksumCalculator().calculateChecksums(fileMetadataCsv)
        _ <- bagit.writeMetadataFilesToBag(bag, checksums)
        // The owner and group in the below command have no effect on the file permissions. It just makes tar idempotent
        _ <- bashCommands.runCommand(s"tar --sort=name --owner=root:0 --group=root:0 --mtime ${java.time.LocalDate.now.toString} -C ${config.efs.rootLocation} -c ./$consignmentId | gzip -n > $tarPath")
        _ <- bashCommands.runCommand(s"sha256sum $tarPath > $tarPath.sha256")
        _ <- s3Files.uploadFiles(config.s3.outputBucket, consignmentId, tarPath)
        _ <- graphQlApi.updateExportLocation(config, consignmentId, s"s3://${config.s3.outputBucket}/$consignmentId.tar.gz")
      } yield ExitCode.Success
    }
}
