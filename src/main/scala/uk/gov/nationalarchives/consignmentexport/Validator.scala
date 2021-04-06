package uk.gov.nationalarchives.consignmentexport

import java.time.LocalDateTime
import java.util.UUID
import cats.implicits._
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.Files
import uk.gov.nationalarchives.consignmentexport.Validator.{ValidatedFfidMetadata, ValidatedFileMetadata}

class Validator(consignmentId: UUID) {
  def validateConsignmentHasFiles(consignmentData: GetConsignment): Either[RuntimeException, Files] = {
    Either.fromOption(consignmentData.files.headOption, new RuntimeException(s"Consignment API returned no files for consignment $consignmentId"))
  }

  def validateConsignmentResult(consignmentResult: Option[GetConsignment]): Either[RuntimeException, GetConsignment] = {
    Either.fromOption(consignmentResult, new RuntimeException(s"No consignment metadata found for consignment $consignmentId"))
  }

  def extractFileMetadata(filesList: List[Files]): Either[RuntimeException, List[ValidatedFileMetadata]] = {
    val fileErrors: Seq[String] = filesList.flatMap(file => {
      val metadataPropertyNames = file.metadata.productElementNames
      val metadataValues = file.metadata.productIterator
      val missingPropertyNames = metadataPropertyNames.zip(metadataValues).filter(propertyNameToValue => {
        propertyNameToValue._2.isInstanceOf[None.type]
      }).map(_._1).toList

      missingPropertyNames match {
        case Nil => None
        case _ => s"${file.fileId} is missing the following properties: ${missingPropertyNames.mkString(", ")}".some
      }
    })

    fileErrors match {
      case Nil => Right(filesList.map(validatedMetadata))
      case _ => Left(new RuntimeException(fileErrors.mkString("\n")))
    }
  }

  private def validatedMetadata(f: Files): ValidatedFileMetadata = ValidatedFileMetadata(f.fileId,
    f.metadata.clientSideFileSize.get,
    f.metadata.clientSideLastModifiedDate.get,
    f.metadata.clientSideOriginalFilePath.get,
    f.metadata.foiExemptionCode.get,
    f.metadata.heldBy.get,
    f.metadata.language.get,
    f.metadata.legalStatus.get,
    f.metadata.rightsCopyright.get,
    f.metadata.sha256ClientSideChecksum.get
  )

  private def validatedFfidMetadata(f: Files): ValidatedFfidMetadata = ValidatedFfidMetadata(f.fileId, // the FFID matches has properties that are Options
    f.ffidMetadata.software,
    f.ffidMetadata.softwareVersion,
    f.ffidMetadata.binarySignatureFileVersion,
    f.ffidMetadata.containerSignatureFileVersion,
    f.ffidMetadata.method,
    f.ffidMetadata.matches, // not sure how to account for the variable number FFIDMetadataInputMatches. Just set an upper limit of matches and leave the rest empty?
    //I guess in the CSV, we could make the number of "matches" columns vary (using a for loop/map) depending on the number that have been returned?
    f.ffidMetadata.datetime
  )
}

object Validator {

  case class ValidatedFfidMetadata(fileId: UUID, software: String, softwareVersion: String, binarySignatureFileVersion: String, containerSignatureFileVersion: String, method: String, matches: List[FFIDMetadataInputMatches], datetime: Long) // the matches list will have a variable number of FFIDMetadataInputMatches

  case class ValidatedFileMetadata(fileId: UUID,
                                   clientSideFileSize: Long,
                                   clientSideLastModifiedDate: LocalDateTime,
                                   clientSideOriginalFilePath: String,
                                   foiExemptionCode: String,
                                   heldBy: String,
                                   language: String,
                                   legalStatus: String,
                                   rightsCopyright: String,
                                   clientSideChecksum: String)

  def apply(consignmentId: UUID): Validator = new Validator(consignmentId)
}
