package uk.gov.nationalarchives.consignmentexport

import java.time.LocalDateTime
import java.util.UUID

import cats.effect.IO
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.Files
import cats.implicits._
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import uk.gov.nationalarchives.consignmentexport.Validator.ValidatedFileMetadata

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
    f.metadata.rightsCopyright.get
  )
}

object Validator {

  case class ValidatedFileMetadata(fileId: UUID, clientSideFileSize: Long, clientSideLastModifiedDate: LocalDateTime, clientSideOriginalFilePath: String, foiExemptionCode: String, heldBy: String, language: String, legalStatus: String, rightsCopyright: String)

  def apply(consignmentId: UUID): Validator = new Validator(consignmentId)
}
