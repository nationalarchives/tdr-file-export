package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.aws.utils.S3Utils
import uk.gov.nationalarchives.consignmentexport.GraphQlApi.FileIdWithPath
import uk.gov.nationalarchives.consignmentexport.Utils._

import scala.language.postfixOps

class S3Files(s3Utils: S3Utils)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def downloadFiles(files: List[FileIdWithPath], bucket: String, consignmentId: UUID, rootLocation: String): IO[Unit] = for {
    _ <- files.map(file => {
      val writeDirectory = file.originalPath.split("/").init.mkString("/")
      new File(s"$rootLocation/$consignmentId/$writeDirectory").mkdirs()
      s3Utils.downloadFiles(bucket, s"$consignmentId/${file.fileId}", s"$rootLocation/$consignmentId/${file.originalPath}".toPath.some)
    }).sequence
    _ <- logger.info(s"Files downloaded from S3 for consignment $consignmentId")
  } yield ()

  def uploadFiles(bucket: String, consignmentId: UUID, tarPath: String): IO[Unit] = for {
    _ <- s3Utils.upload(bucket, s"$consignmentId.tar.gz", tarPath.toPath)
    _ <- s3Utils.upload(bucket, s"$consignmentId.tar.gz.sha256", s"$tarPath.sha256".toPath)
    _ <- logger.info(s"Files uploaded to S3 for consignment $consignmentId")
  } yield ()
}

object S3Files {
  def apply(s3Utils: S3Utils)(implicit logger: SelfAwareStructuredLogger[IO]): S3Files = new S3Files(s3Utils)(logger)
}
