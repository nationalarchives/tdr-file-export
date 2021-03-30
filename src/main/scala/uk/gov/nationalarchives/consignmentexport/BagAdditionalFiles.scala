package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter
import uk.gov.nationalarchives.consignmentexport.Validator.{ValidatedFileMetadata, ValidatedFfidMetadata}

class BagAdditionalFiles(rootDirectory: Path) {

  def createFileMetadataCsv(fileMetadataList: List[ValidatedFileMetadata]): IO[File] = {
    val header = List("Filepath", "Filesize", "RightsCopyright", "LegalStatus", "HeldBy", "Language", "FoiExemptionCode", "LastModified")
    val fileMetadataRows = fileMetadataList.map(f => List(f.clientSideOriginalFilePath, f.clientSideFileSize, f.rightsCopyright, f.legalStatus, f.heldBy, f.language, f.foiExemptionCode, f.clientSideLastModifiedDate))
    writeToCsv("file-metadata.csv", header, fileMetadataRows)
  }

  def createFfidMetadataCsv(ffidMetadataList: List[ValidatedFfidMetadata]): IO[File] = {
    //I assume for the matches, there's going to be "Extension1", "IdentificationBasis1", "PUID1", "Extension2", "IdentificationBasis2", "PUID2",..."DateTime" maybe?
    val header = List("Software", "SoftwareVersion", "BinarySignatureFileVersion", "ContainerSignatureFileVersion", "Method", "Extension", "IdentificationBasis", "PUID", "DateTime")

    val ffidMetadataRows = ffidMetadataList.map{
      f =>
        val rowFirstPart = List(f.software, f.softwareVersion, f.binarySignatureFileVersion, f.containerSignatureFileVersion, f.method)
        val rowSecondPart = f.matches // maybe the max number of matches could feed into the header length
        val rowThirdPart = List(f.datetime)

      rowFirstPart ++ rowSecondPart ++ rowThirdPart
    }

    writeToCsv("ffid-metadata.csv", header, ffidMetadataRows)
  }


  private def writeToCsv(fileName: String, header: List[String], metadataRows: List[List[Any]]): IO[File] = {
    val file = new File(s"$rootDirectory/$fileName")
    val writer = CSVWriter.open(file)
    writer.writeAll(List(header :: metadataRows))
    writer.close()
    IO(file)
  }
}

object BagAdditionalFiles {
  def apply(rootDirectory: Path): BagAdditionalFiles = new BagAdditionalFiles(rootDirectory)
}
