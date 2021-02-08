package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.time.LocalDateTime
import java.util.UUID

import uk.gov.nationalarchives.consignmentexport.Utils.PathUtils

import scala.io.Source
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

class BagAdditionalFilesSpec extends ExportSpec {
  "fileMetadataCsv" should "produce a file with the correct rows" in {
    val bagAdditionalFiles = BagAdditionalFiles(getClass.getResource(".").getPath.toPath)
    val lastModified = LocalDateTime.parse("2021-02-03T10:33:30.414")
    val metadata = ValidatedFileMetadata(
      UUID.randomUUID(),
      1L,
      lastModified,
      "originalPath",
      "foiExemption",
      "heldBy",
      "language",
      "legalStatus",
      "rightsCopyright"
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


}