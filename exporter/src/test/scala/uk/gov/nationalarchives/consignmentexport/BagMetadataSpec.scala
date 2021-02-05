package uk.gov.nationalarchives.consignmentexport

import java.time.ZonedDateTime
import java.util.UUID

import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.{Series, TransferringBody}
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.Utils._

class BagMetadataSpec extends ExportSpec {

  private val fixedDateTime = ZonedDateTime.now()
  private val userId = UUID.randomUUID()
  private val series = Series(Some("series-code"))
  private val transferringBody = TransferringBody(Some("tb-code"))
  private val consignment = GetConsignment(
    userId, Some(fixedDateTime), Some(fixedDateTime), Some(fixedDateTime), Some(series), Some(transferringBody), List()
  )
  private val userRepresentation = new UserRepresentation()
  userRepresentation.setFirstName("FirstName")
  userRepresentation.setLastName("LastName")

  "the getBagMetadata method" should "return the correct bag metadata for the given consignment id" in {

    val mockKeycloakClient = mock[KeycloakClient]

    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])
    val bagMetadata = BagMetadata(mockKeycloakClient).generateMetadata(consignment).unsafeRunSync()
    bagMetadata.get("Consignment-Series").get(0) should be("series-code")
    bagMetadata.get("Source-Organization").get(0) should be("tb-code")
    bagMetadata.get("Consignment-StartDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Consignment-CompletedDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Contact-Name").get(0) should be("FirstName LastName")
    bagMetadata.get("Consignment-ExportDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
  }

  "the getBagMetadata method" should "return the bag metadata containing other consignment metadata properties if a property is missing" in {
    val missingPropertyKey = "Consignment-StartDate"
    val incompleteConsignment = GetConsignment(
      userId, None, Some(fixedDateTime), Some(fixedDateTime), Some(series), Some(transferringBody), List()
    )
    val mockKeycloakClient = mock[KeycloakClient]

    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])

    val bagMetadata = BagMetadata(mockKeycloakClient).generateMetadata(incompleteConsignment).unsafeRunSync()
    bagMetadata.contains(missingPropertyKey) should be(false)

    bagMetadata.contains("Consignment-Series") should be(true)
    bagMetadata.contains("Source-Organization") should be(true)
    bagMetadata.contains("Consignment-CompletedDate") should be(true)
    bagMetadata.contains("Contact-Name") should be(true)
    bagMetadata.contains("Consignment-ExportDate") should be(true)
  }

  "the getBagMetadata method" should "throw an exception if incomplete user details are found" in {
    val mockKeycloakClient = mock[KeycloakClient]
    val incompleteUserRepresentation = new UserRepresentation()
    incompleteUserRepresentation.setLastName("LastName")

    doAnswer(() => incompleteUserRepresentation).when(mockKeycloakClient).getUserDetails(userId.toString)

    val exception = intercept[RuntimeException] {
      BagMetadata(mockKeycloakClient).generateMetadata(consignment).unsafeRunSync()
    }
    exception.getMessage should equal(s"Incomplete details for user $userId")
  }
}
