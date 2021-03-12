package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.domain.Bag
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

import scala.jdk.CollectionConverters._

class ChecksumValidator()(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def validateFileChecksums(bag: Bag, validatedFileMetadata: List[ValidatedFileMetadata]): IO[List[UUID]] = {
    val bagitGeneratedChecksums = bag.getPayLoadManifests.asScala.head.getFileToChecksumMap.values

    IO(validatedFileMetadata.filterNot(fm => bagitGeneratedChecksums.contains(fm.clientSideChecksum)).map(_.fileId))
  }
}

object ChecksumValidator {

  def apply()(implicit logger: SelfAwareStructuredLogger[IO]): ChecksumValidator = new ChecksumValidator()(logger)
}
