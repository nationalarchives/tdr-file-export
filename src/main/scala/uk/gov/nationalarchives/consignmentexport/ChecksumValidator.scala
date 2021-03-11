package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.domain.Bag
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

import scala.jdk.CollectionConverters._

class ChecksumValidator(consignmentId: UUID)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def validateFileChecksums(bag: Bag,
                            validatedFileMetadata: List[ValidatedFileMetadata]): Either[RuntimeException, IO[Unit]] = {

    val bagitGeneratedChecksums = bag.getPayLoadManifests.asScala.head.getFileToChecksumMap.values()

    val checkSumMismatches: List[UUID] = validatedFileMetadata.filter(fm =>
      !bagitGeneratedChecksums.contains(fm.clientSideChecksum)).map(_.fileId)

    checkSumMismatches match {
      case Nil => Right(logger.info(s"File checksums for consignment $consignmentId validated"))
      case _ => Left(new RuntimeException(s"Checksum mismatch for file(s): ${checkSumMismatches.mkString("\n")}"))
    }
  }
}

object ChecksumValidator {

  def apply(consignmentId: UUID)(implicit logger: SelfAwareStructuredLogger[IO]): ChecksumValidator = new ChecksumValidator(consignmentId)(logger)
}
