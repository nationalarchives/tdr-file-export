package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.time.LocalDateTime
import java.util.UUID

import graphql.codegen.GetExportFileMetadata.getFileMetadataForConsignmentExport.GetConsignment.FileMetadata
import uk.gov.nationalarchives.consignmentexport.Utils.PathUtils

import scala.io.Source
import cats.implicits._

class BagAdditionalFilesSpec extends ExportSpec {
  "fileMetadataCsv" should "produce a file with the correct rows if all fields are set" in {
    val bagAdditionalFiles = BagAdditionalFiles(getClass.getResource(".").getPath.toPath)
    val lastModified = LocalDateTime.parse("2021-02-03T10:33:30.414")
    val metadata = FileMetadata(
      UUID.randomUUID(),
      1L.some,
      lastModified.some,
      "originalPath".some,
      "foiExemption".some,
      "heldBy".some,
      "language".some,
      "legalStatus".some,
      "rightsCopyright".some
    )
    val file = bagAdditionalFiles.fileMetadataCsv(List(metadata)).unsafeRunSync()

    val source = Source.fromFile(file)
    val csvLines = source.getLines().toList
    val header = csvLines.head
    val rest = csvLines.tail
    header should equal("Filepath,Filesize,RightsCopyright,LegalStatus,HeldBy,Language,FoiExemptionCode,LastModified")
    rest.length should equal(1)
    rest.head should equal(s"originalPath,1,rightsCopyright,legalStatus,heldBy,language,foiExemption,2021-02-03T10:33:30.414")
    source.close()
    new File("exporter/src/test/resources/file-metadata.csv").delete()
  }

  "fileMetadataCsv" should "return an error  if some of the fields are not set" in {
    val bagAdditionalFiles = BagAdditionalFiles(getClass.getResource(".").getPath.toPath)
    val fileId = UUID.randomUUID()
    val fileIdTwo = UUID.randomUUID()
    val metadata = FileMetadata(
      fileId,
      1L.some,
      Option.empty,
      Option.empty,
      "foiExemption".some,
      "heldBy".some,
      "language".some,
      "legalStatus".some,
      "rightsCopyright".some
    )
    val metadataTwo = FileMetadata(
      fileIdTwo,
      1L.some,
      LocalDateTime.parse("2021-02-03T10:33:30.414").some,
      "clientSideOriginalFilePath".some,
      Option.empty,
      Option.empty,
      Option.empty,
      "legalStatus".some,
      "rightsCopyright".some
    )
    val file: Either[Throwable, File] = bagAdditionalFiles.fileMetadataCsv(List(metadata, metadataTwo)).attempt.unsafeRunSync()
    file.left.value.getMessage should equal(
      s"$fileId is missing the following properties: clientSideLastModifiedDate, clientSideOriginalFilePath\n$fileIdTwo is missing the following properties: foiExemptionCode, heldBy, language"
    )
  }
  "fileMetadataCsv" should "return an error  if one file has some of the fields unset and one file has all fields set" in {
    val bagAdditionalFiles = BagAdditionalFiles(getClass.getResource(".").getPath.toPath)
    val fileId = UUID.randomUUID()
    val metadata = FileMetadata(
      UUID.randomUUID(),
      1L.some,
      LocalDateTime.parse("2021-02-03T10:33:30.414").some,
      "clientSideOriginalFilePath".some,
      "foiExemption".some,
      "heldBy".some,
      "language".some,
      "legalStatus".some,
      "rightsCopyright".some
    )
    val metadataTwo = FileMetadata(
      fileId,
      1L.some,
      LocalDateTime.parse("2021-02-03T10:33:30.414").some,
      "clientSideOriginalFilePath".some,
      "foiExemption".some,
      "heldBy".some,
      Option.empty,
      Option.empty,
      "rightsCopyright".some
    )
    val file: Either[Throwable, File] = bagAdditionalFiles.fileMetadataCsv(List(metadata, metadataTwo)).attempt.unsafeRunSync()
    file.left.value.getMessage should equal(s"$fileId is missing the following properties: language, legalStatus")
  }
}
