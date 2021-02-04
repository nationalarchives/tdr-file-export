package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Path

import cats.effect.IO
import com.github.tototoshi.csv.CSVWriter
import graphql.codegen.GetExportFileMetadata.getFileMetadataForConsignmentExport.GetConsignment.FileMetadata

class BagAdditionalFiles(rootDirectory: Path) {

  implicit class FileMetadataUtils(metadata: FileMetadata) {
    def toCsvList = List(
      metadata.clientSideOriginalFilePath.get,
      metadata.clientSideFileSize.get,
      metadata.rightsCopyright.get,
      metadata.legalStatus.get,
      metadata.heldBy.get,
      metadata.language.get,
      metadata.foiExemptionCode.get,
      metadata.clientSideLastModifiedDate.get
    )
  }

  def fileMetadataCsv(metadataList: List[FileMetadata]): IO[File] = for {
      dataRowsOrError <-
        IO(
          metadataList.collect(metadata => {
            metadata.productElementNames.zip(metadata.productIterator).collect(propertyNameToValue => propertyNameToValue._2 match {
              case x: Option[_] if x.isEmpty => propertyNameToValue._1
            }).toList match {
              case head :: next => Left(s"${metadata.fileId} is missing the following properties: ${(head :: next).mkString(", ")}")
              case Nil => Right(metadata.toCsvList)
            }
          })
        )
      file <- {
        val (errors, metadata) = dataRowsOrError.partitionMap(identity)
        errors match {
          case ::(head, next) => IO.raiseError(new Exception((head :: next).mkString("\n")))
          case Nil =>
            val file = new File(s"$rootDirectory/file-metadata.csv")
            val writer = CSVWriter.open(file)
            writer.writeAll(List("Filepath", "Filesize", "RightsCopyright", "LegalStatus", "HeldBy", "Language", "FoiExemptionCode", "LastModified") :: metadata)
            writer.close()
            IO(file)
        }
      }
    } yield file
}

object BagAdditionalFiles {
  def apply(rootDirectory: Path): BagAdditionalFiles = new BagAdditionalFiles(rootDirectory)
}
