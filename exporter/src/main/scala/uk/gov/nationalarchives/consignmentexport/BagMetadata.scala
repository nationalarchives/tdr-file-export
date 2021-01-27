package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.domain.Metadata
import graphql.codegen.GetConsignmentExport
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.consignmentexport.BagMetadata._
import uk.gov.nationalarchives.consignmentexport.Config.{Configuration, config}

class BagMetadata(graphQlApi: GraphQlApi, keycloakClient: KeycloakClient)(implicit val logger: SelfAwareStructuredLogger[IO]) {
  def getBagMetadata(consignmentId: UUID, config: Configuration): IO[Metadata] = for {
   consignment <- graphQlApi.getConsignmentExport(config, consignmentId)
   metadata = new Metadata

   //Question throw exception if fail to add consignment info?
   consignmentInfoAdded = consignment match {
     case Some(consignment) => addConsignmentMetadata(consignment, metadata)
     case None => false
   }

  } yield metadata

  private def addConsignmentMetadata(consignment: GetConsignmentExport.getConsignmentExport.GetConsignment, metadata: Metadata): Boolean = {
    //Question if a value is missing from the consignment info, should a default value be added, or ignore, or throw exeception?
    for {
      series <- consignment.series
      sc <- series.code
    } yield metadata.add(ConsignmentSeriesKey, sc)

    for {
      body <- consignment.transferringBody
      bc <- body.code
    } yield metadata.add(SourceOrganisationKey, bc)

    for {
      startDate <- consignment.dateTime
      sd = startDate.toString
    } yield metadata.add(ConsignmentStartDateKey, sd)

    for {
      completedDate <- consignment.transferInitiatedDatetime
      cd = completedDate.toString
    } yield metadata.add(ConsignmentCompletedDateKey, cd)

    for {
      exportDate <- consignment.exportDatetime
      ed = exportDate.toString
    } yield metadata.add(ConsignmentExportDateKey, ed)

    metadata.add(ContactNameKey, getContactName(consignment.userid))
  }

  private def getContactName(userId: UUID): String =  {
      val userDetails = keycloakClient.getUserDetails(userId.toString)
      s"${userDetails.getFirstName} ${userDetails.getLastName}"
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
