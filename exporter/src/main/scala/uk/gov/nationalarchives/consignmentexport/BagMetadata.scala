package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.domain.Metadata
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.BagMetadata._
import uk.gov.nationalarchives.consignmentexport.Config.Configuration

class BagMetadata(graphQlApi: GraphQlApi, keycloakClient: KeycloakClient)(implicit val logger: SelfAwareStructuredLogger[IO]) {
  def getBagMetadata(consignmentId: UUID, config: Configuration): IO[Metadata] = for {
   consignment <- graphQlApi.getConsignmentMetadata(config, consignmentId)
   consignmentDetails = consignment match {
     case Some(consignment) => getConsignmentDetails(consignment)
     case None => throw new RuntimeException(s"No consignment metadata found for consignment $consignmentId")
   }

  } yield generateMetadata(consignmentDetails)

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
      cd = createdDate.toString
    } yield cd

    val completedDatetime = for {
      completedDate <- consignment.transferInitiatedDatetime
      cd = completedDate.toString
    } yield cd

    val exportDatetime = for {
      exportDate <- consignment.exportDatetime
      ed = exportDate.toString
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

  private def generateMetadata(details: Map[String, Option[String]]): Metadata = {
    val metadata = new Metadata

    details.map(e => {
      e._2 match {
        case Some(_) => metadata.add(e._1, e._2.get)
        case None => //For now do nothing is property is missing
      }
    })
    metadata
  }

  private def getContactName(userId: UUID): String =  {
      val userDetails = getUserDetails(userId.toString)
      s"${userDetails.getFirstName} ${userDetails.getLastName}"
  }

  private def getUserDetails(userId: String): UserRepresentation = {
    val userDetails = keycloakClient.getUserDetails(userId)
    userDetailsComplete(userDetails) match {
      case true => userDetails
      case _ => throw new RuntimeException(s"Incomplete details for user $userId")
    }
  }

  private def userDetailsComplete(userDetails: UserRepresentation): Boolean = {
    userDetails.getFirstName != null && userDetails.getLastName != null
  }
}

object BagMetadata {
  private val SourceOrganisationKey = "Source-Organization"
  private val ConsignmentSeriesKey = "Consignment-Series"
  private val ConsignmentStartDateKey = "Consignment-StartDate"
  private val ConsignmentCompletedDateKey = "Consignment-CompletedDate"
  private val ContactNameKey = "Contact-Name"
  private val ConsignmentExportDateKey = "Consignment-ExportDate"

  def apply(
             graphQlApi: GraphQlApi,
             keycloakClient: KeycloakClient)(implicit logger: SelfAwareStructuredLogger[IO]): BagMetadata = new BagMetadata(graphQlApi, keycloakClient)(logger)
}
