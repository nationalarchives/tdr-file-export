package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

class BagAdditionalFiles(rootDirectory: Path) {

  def fileMetadataCsv(metadataList: List[ValidatedFileMetadata]): IO[File] = {
    val file = new File(s"$rootDirectory/file-metadata.csv")
    val writer = CSVWriter.open(file)
    val metadata = metadataList.map(f => List(f.clientSideOriginalFilePath, f.clientSideFileSize, f.rightsCopyright, f.legalStatus, f.heldBy, f.language, f.foiExemptionCode, f.clientSideLastModifiedDate))
    writer.writeAll(List("Filepath", "Filesize", "RightsCopyright", "LegalStatus", "HeldBy", "Language", "FoiExemptionCode", "LastModified") :: metadata)
    writer.close()
    IO(file)
  }
}

object BagAdditionalFiles {
  def apply(rootDirectory: Path): BagAdditionalFiles = new BagAdditionalFiles(rootDirectory)
}
