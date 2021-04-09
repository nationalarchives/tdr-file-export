package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Path
import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter
import uk.gov.nationalarchives.consignmentexport.Validator.{ValidatedFFIDMetadata, ValidatedFileMetadata}

class BagAdditionalFiles(rootDirectory: Path) {

  def createFileMetadataCsv(fileMetadataList: List[ValidatedFileMetadata]): IO[File] = {
    val header = List("Filepath", "Filesize", "RightsCopyright", "LegalStatus", "HeldBy", "Language", "FoiExemptionCode", "LastModified")
    val fileMetadataRows = fileMetadataList.map(f => List(f.clientSideOriginalFilePath, f.clientSideFileSize, f.rightsCopyright, f.legalStatus, f.heldBy, f.language, f.foiExemptionCode, f.clientSideLastModifiedDate))
    writeToCsv("file-metadata.csv", header, fileMetadataRows)
  }

  def createFfidMetadataCsv(ffidMetadataList: List[ValidatedFFIDMetadata]): IO[File] = {
    val header = List("Filepath","Extension","PUID","FFID-Software","FFID-SoftwareVersion","FFID-BinarySignatureFileVersion","FFID-ContainerSignatureFileVersion")
    val metadataRows = ffidMetadataList.map(f => {
      List(f.filePath, f.extension, f.puid, f.software, f.softwareVersion, f.binarySignatureFileVersion, f.containerSignatureFileVersion)
    })
    writeToCsv("ffid-metadata.csv", header, metadataRows)
  }

  private def writeToCsv(fileName: String, header: List[String], metadataRows: List[List[Any]]): IO[File] = {
    val file = new File(s"$rootDirectory/$fileName")
    val writer = CSVWriter.open(file)
    writer.writeAll(header :: metadataRows)
    writer.close()
    IO(file)
  }
}

object BagAdditionalFiles {
  def apply(rootDirectory: Path): BagAdditionalFiles = new BagAdditionalFiles(rootDirectory)
}
