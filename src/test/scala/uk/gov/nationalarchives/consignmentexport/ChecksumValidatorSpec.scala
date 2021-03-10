package uk.gov.nationalarchives.consignmentexport

import java.time.LocalDateTime
import java.util.UUID

import cats.effect.IO
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

class ChecksumValidatorSpec extends ExportSpec {

  private val consignmentId = UUID.randomUUID()
  private val fileId1 = UUID.randomUUID()
  private val fileId2 = UUID.randomUUID()
  private val fileId3 = UUID.randomUUID()

  private val metadata1 = ValidatedFileMetadata(
    fileId1,
    1L,
    LocalDateTime.parse("2021-02-03T10:33:30.414"),
    "clientSideOriginalFilePath",
    "foiExemption",
    "heldBy",
    "language",
    "legalStatus",
    "rightsCopyright",
    "clientSideChecksum1")

  private val metadata2 = ValidatedFileMetadata(
    fileId2,
    1L,
    LocalDateTime.parse("2021-02-03T10:33:30.414"),
    "clientSideOriginalFilePath",
    "foiExemption",
    "heldBy",
    "language",
    "legalStatus",
    "rightsCopyright",
    "clientSideChecksum2")

  private val metadata3 = ValidatedFileMetadata(
    fileId3,
    1L,
    LocalDateTime.parse("2021-02-03T10:33:30.414"),
    "clientSideOriginalFilePath",
    "foiExemption",
    "heldBy",
    "language",
    "legalStatus",
    "rightsCopyright",
    "clientSideChecksum3")

  "validateFilesChecksums" should "should complete successfully if there are no mismatches between checksum values" in {
    val checksums = List(
      "clientSideChecksum1",
      "clientSideChecksum2"
    )

    val attempt: Either[Throwable, IO[Unit]] = ChecksumValidator(consignmentId).validateFilesChecksums(checksums, List(metadata1, metadata2))
    attempt.isRight should be(true)
  }

  "validateFilesChecksums" should "return an error if there is a mismatch between checksum values" in {
    val checksums = List(
      "clientSideChecksum1",
      "someDifferentClientSideChecksum1",
      "someDifferentClientSideChecksum2"
    )

    val attempt: Either[Throwable, IO[Unit]] = ChecksumValidator(consignmentId).validateFilesChecksums(checksums, List(metadata1, metadata2, metadata3))
    attempt.left.value.getMessage should equal(s"Checksum mismatch for file(s): $fileId2\n$fileId3")
  }

}
