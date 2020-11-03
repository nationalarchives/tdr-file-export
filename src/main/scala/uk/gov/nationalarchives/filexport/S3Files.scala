package uk.gov.nationalarchives.filexport

import java.nio.file.{Path, Paths}
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import graphql.codegen.GetFiles.getFiles.Data
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.aws.utils.S3Utils
import Utils._

import scala.language.postfixOps
import scala.sys.process._

class S3Files(s3Utils: S3Utils)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def downloadFiles(data: Data, bucket: String, consignmentId: UUID, rootLocation: String): IO[Unit] = for {
    _ <- IO.pure(s"mkdir -p $rootLocation/$consignmentId" !!)
    _ <- data.getFiles.fileIds.map(fileId => s3Utils.downloadFiles(bucket, s"/$consignmentId/$fileId", s"$rootLocation/$consignmentId/$fileId".toPath.some)).sequence
    _ <- logger.info("Files downloaded from S3")
  } yield ()

  def uploadFiles(bucket: String, consignmentId: UUID, tarPath: String): IO[Unit] = for {
    _ <- s3Utils.upload(bucket, s"$consignmentId.tar.gz", tarPath.toPath)
    _ <- s3Utils.upload(bucket, s"$consignmentId.tar.gz.sha256", s"$tarPath.sha256".toPath)
    _ <- logger.info("Files uploaded to S3")
  } yield ()
}

object S3Files {
  def apply(s3Utils: S3Utils)(implicit logger: SelfAwareStructuredLogger[IO]): S3Files = new S3Files(s3Utils)(logger)
}
