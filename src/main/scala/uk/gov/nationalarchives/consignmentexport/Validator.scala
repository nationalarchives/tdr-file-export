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

  def validateFileMetadataNotEmpty(filesList: List[Files]): Either[RuntimeException, List[ValidatedFileMetadata]] = {
    filesList.flatMap(file => {
      file.metadata.productElementNames.zip(file.metadata.productIterator).collect(propertyNameToValue => propertyNameToValue._2 match {
        case x: Option[_] if x.isEmpty => propertyNameToValue._1
      }).toList match {
        case head :: next => s"${file.fileId} is missing the following properties: ${(head :: next).mkString(", ")}".some
        case Nil => none
      }
    }) match {
      case head :: next => Left(new RuntimeException((head :: next).mkString("\n")))
      case Nil =>
        def validatedMetadata(f: Files): ValidatedFileMetadata = ValidatedFileMetadata(f.fileId,
          f.metadata.clientSideFileSize.get,
          f.metadata.clientSideLastModifiedDate.get,
          f.metadata.clientSideOriginalFilePath.get,
          f.metadata.foiExemptionCode.get,
          f.metadata.heldBy.get,
          f.metadata.language.get,
          f.metadata.legalStatus.get,
          f.metadata.rightsCopyright.get
        )
        Right(filesList.map(validatedMetadata))
    }
  }
}

object Validator {

  case class ValidatedFileMetadata(fileId: UUID, clientSideFileSize: Long, clientSideLastModifiedDate: LocalDateTime, clientSideOriginalFilePath: String, foiExemptionCode: String, heldBy: String, language: String, legalStatus: String, rightsCopyright: String)

  def apply(consignmentId: UUID): Validator = new Validator(consignmentId)
}
