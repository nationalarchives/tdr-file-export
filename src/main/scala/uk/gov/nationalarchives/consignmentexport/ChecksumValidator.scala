package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import Validator.ValidatedFileMetadata
import cats.effect.IO
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class ChecksumValidator(consignmentId: UUID)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def validateFilesChecksums(checksums: List[String],
                             validatedFileMetadata: List[ValidatedFileMetadata]): Either[RuntimeException, IO[Unit]] = {

    val checkSumMismatches: List[UUID] = validatedFileMetadata.filter(fm =>
      !checksums.contains(fm.clientSideChecksum)).map(_.fileId)

    checkSumMismatches match {
      case Nil => Right(logger.info(s"Checksums validated for consignment $consignmentId"))
      case _ => Left(new RuntimeException(s"Checksum mismatch for file(s): ${checkSumMismatches.mkString("\n")}"))
    }
  }
}

object ChecksumValidator {

  def apply(consignmentId: UUID)(implicit logger: SelfAwareStructuredLogger[IO]): ChecksumValidator = new ChecksumValidator(consignmentId)(logger)
}
