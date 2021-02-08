package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.domain.Metadata
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.BagMetadata._
import uk.gov.nationalarchives.consignmentexport.Config.Configuration
import uk.gov.nationalarchives.consignmentexport.Utils._

class BagMetadata(keycloakClient: KeycloakClient)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  implicit class UserRepresentationUtils(value: UserRepresentation) {
    private def isStringNullOrEmpty(s: String): Boolean = s == null || s.trim.isEmpty

    def isUserRepresentationComplete: Boolean = {
      !isStringNullOrEmpty(value.getFirstName) && !isStringNullOrEmpty(value.getLastName)
    }
  }

  private def getConsignmentDetails(consignment: GetConsignment): Map[String, Option[String]] = {
    val seriesCode = for {
      series <- consignment.series
      sc <- series.code
    } yield sc

    val bodyCode = for {
      body <- consignment.transferringBody
      bc <- body.code
    } yield bc

    val startDatetime = for {
      createdDate <- consignment.createdDatetime
      cd = createdDate.toFormattedPrecisionString
    } yield cd

    val completedDatetime = for {
      completedDate <- consignment.transferInitiatedDatetime
      cd = completedDate.toFormattedPrecisionString
    } yield cd

    val exportDatetime = for {
      exportDate <- consignment.exportDatetime
      ed = exportDate.toFormattedPrecisionString
    } yield ed

    val contactName = getContactName(consignment.userid)

    Map(
      ConsignmentSeriesKey -> seriesCode,
      SourceOrganisationKey -> bodyCode,
      ConsignmentStartDateKey -> startDatetime,
      ConsignmentCompletedDateKey -> completedDatetime,
      ConsignmentExportDateKey -> exportDatetime,
      ContactNameKey -> Some(contactName)
    )
  }

  def generateMetadata(consignmentId: UUID, consignment: GetConsignment): IO[Metadata] = {
    val details: Map[String, Option[String]] = getConsignmentDetails(consignment)
    val metadata = new Metadata

    details.map(e => {
      e._2 match {
        case Some(_) => metadata.add(e._1, e._2.get)
        case None => throw new RuntimeException(s"Missing consignment metadata property ${e._1} for consignment $consignmentId")
      }
    })
    IO(metadata)
  }

  private def getContactName(userId: UUID): String =  {
      val userDetails = getUserDetails(userId.toString)
      s"${userDetails.getFirstName} ${userDetails.getLastName}"
  }

  private def getUserDetails(userId: String): UserRepresentation = {
    val userDetails = keycloakClient.getUserDetails(userId)
    userDetails.isUserRepresentationComplete match {
      case true => userDetails
      case _ => throw new RuntimeException(s"Incomplete details for user $userId")
    }
  }
}

object BagMetadata {
  private val SourceOrganisationKey = "Source-Organization"
  private val ConsignmentSeriesKey = "Consignment-Series"
  private val ConsignmentStartDateKey = "Consignment-StartDate"
  private val ConsignmentCompletedDateKey = "Consignment-CompletedDate"
  private val ContactNameKey = "Contact-Name"
  private val ConsignmentExportDateKey = "Consignment-ExportDate"

  def apply(keycloakClient: KeycloakClient)(implicit logger: SelfAwareStructuredLogger[IO]): BagMetadata = new BagMetadata(keycloakClient)(logger)
}
