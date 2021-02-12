package uk.gov.nationalarchives.consignmentexport

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import cats.implicits._
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.Files.Metadata
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.{Files, Series, TransferringBody}
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

class ValidatorSpec extends ExportSpec {

  private val completeFileMetadata = Files(
    UUID.randomUUID(),
    Metadata(
      1L.some,
      LocalDateTime.now().some,
      "originalPath".some,
      "foiExemption".some,
      "heldBy".some,
      "language".some,
      "legalStatus".some,
      "rightsCopyright".some)
  )

  private def consignment(consignmentId: UUID, metadata: List[Files] = List(completeFileMetadata)): GetConsignment = GetConsignment(
    consignmentId,
    ZonedDateTime.now().some,
    ZonedDateTime.now().some,
    ZonedDateTime.now().some,
    Series("series-code".some).some,
    TransferringBody("tb-code".some).some,
    metadata
  )

  "validateConsignmentHasFiles" should "return an error if the consignment has no files" in {
    val consignmentId = UUID.randomUUID()
    val validator = Validator(consignmentId)
    val attempt: Either[Throwable, Files] = validator.validateConsignmentHasFiles(consignment(consignmentId, List()))
    attempt.left.value.getMessage should equal(s"Consignment API returned no files for consignment $consignmentId")
  }

  "validateConsignmentHasFiles" should "not return an error if the consignment has files" in {
    val consignmentId = UUID.randomUUID()
    val validator = Validator(consignmentId)
    val attempt: Either[Throwable, Files] = validator.validateConsignmentHasFiles(consignment(consignmentId))
    attempt.isRight should be(true)
  }

  "validateConsignmentResult" should "return an error if the consignment data is not defined" in {
    val consignmentId = UUID.randomUUID()
    val validator = Validator(consignmentId)
    val attempt = validator.validateConsignmentResult(none)
    attempt.left.value.getMessage should equal(s"No consignment metadata found for consignment $consignmentId")
  }

  "validateConsignmentResult" should "not return an error if the consignment data is defined" in {
    val consignmentId = UUID.randomUUID()
    val validator = Validator(consignmentId)
    val attempt = validator.validateConsignmentResult(consignment(consignmentId).some)
    attempt.isRight should be(true)
  }

  "validateFileMetadataNotEmpty" should "not return an error if all of the fields are set" in {
    val validator = Validator(UUID.randomUUID())
    val attempt: Either[Throwable, List[ValidatedFileMetadata]] = validator.validateFileMetadataNotEmpty(List(completeFileMetadata))
    attempt.right.value.length should equal(1)
  }

  "validateFileMetadataNotEmpty" should "return an error  if some of the fields are not set" in {
    val validator = Validator(UUID.randomUUID())
    val fileId = UUID.randomUUID()
    val fileIdTwo = UUID.randomUUID()
    val metadata = Files(
      fileId,
      Metadata(
        1L.some,
        Option.empty,
        Option.empty,
        "foiExemption".some,
        "heldBy".some,
        "language".some,
        "legalStatus".some,
        "rightsCopyright".some
      )
    )
    val metadataTwo = Files(
      fileIdTwo,
      Metadata(
        1L.some,
        LocalDateTime.parse("2021-02-03T10:33:30.414").some,
        "clientSideOriginalFilePath".some,
        Option.empty,
        Option.empty,
        Option.empty,
        "legalStatus".some,
        "rightsCopyright".some
      )
    )
    val file: Either[Throwable, List[ValidatedFileMetadata]] = validator.validateFileMetadataNotEmpty(List(metadata, metadataTwo))
    file.left.value.getMessage should equal(
      s"$fileId is missing the following properties: clientSideLastModifiedDate, clientSideOriginalFilePath\n$fileIdTwo is missing the following properties: foiExemptionCode, heldBy, language"
    )
  }
  "fileMetadataCsv" should "return an error  if one file has some of the fields unset and one file has all fields set" in {
    val validator = Validator(UUID.randomUUID())
    val fileId = UUID.randomUUID()
    val metadata = Files(
      UUID.randomUUID(),
      Metadata(
        1L.some,
        LocalDateTime.parse("2021-02-03T10:33:30.414").some,
        "clientSideOriginalFilePath".some,
        "foiExemption".some,
        "heldBy".some,
        "language".some,
        "legalStatus".some,
        "rightsCopyright".some
      )
    )
    val metadataTwo = Files(
      fileId,
      Metadata(
        1L.some,
        LocalDateTime.parse("2021-02-03T10:33:30.414").some,
        "clientSideOriginalFilePath".some,
        "foiExemption".some,
        "heldBy".some,
        Option.empty,
        Option.empty,
        "rightsCopyright".some
      )
    )
    val file: Either[Throwable, List[ValidatedFileMetadata]] = validator.validateFileMetadataNotEmpty(List(metadata, metadataTwo))
    file.left.value.getMessage should equal(s"$fileId is missing the following properties: language, legalStatus")
  }
}
