package uk.gov.nationalarchives.consignmentexport

import java.time.ZonedDateTime
import java.util.UUID

import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.{Series, TransferringBody}
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.BuildInfo.version
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
    val consignmentId = UUID.randomUUID()
    val mockKeycloakClient = mock[KeycloakClient]

    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])
    val bagMetadata = BagMetadata(mockKeycloakClient).generateMetadata(consignmentId, consignment, fixedDateTime).unsafeRunSync()

    bagMetadata.get("Consignment-Series").get(0) should be("series-code")
    bagMetadata.get("Source-Organization").get(0) should be("tb-code")
    bagMetadata.get("Consignment-StartDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Consignment-CompletedDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Contact-Name").get(0) should be("FirstName LastName")
    bagMetadata.get("Consignment-ExportDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Bag-Creator").get(0) should be(version)
  }

  "the getBagMetadata method" should "throw an exception if a consignment metadata property is missing" in {
    val missingPropertyKey = "Consignment-StartDate"
    val consignmentId = UUID.randomUUID()
    val incompleteConsignment = GetConsignment(
      userId, None, Some(fixedDateTime), Some(fixedDateTime), Some(series), Some(transferringBody), List()
    )
    val mockKeycloakClient = mock[KeycloakClient]

    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])

    val exception = intercept[RuntimeException] {
      BagMetadata(mockKeycloakClient).generateMetadata(consignmentId, incompleteConsignment, fixedDateTime).unsafeRunSync()
    }
    exception.getMessage should equal(s"Missing consignment metadata property $missingPropertyKey for consignment $consignmentId")
  }


  "the getBagMetadata method" should "throw an exception if incomplete user details are found" in {
    val mockKeycloakClient = mock[KeycloakClient]
    val consignmentId = UUID.randomUUID()
    val incompleteUserRepresentation = new UserRepresentation()
    incompleteUserRepresentation.setLastName("LastName")

    doAnswer(() => incompleteUserRepresentation).when(mockKeycloakClient).getUserDetails(userId.toString)

    val exception = intercept[RuntimeException] {
      BagMetadata(mockKeycloakClient).generateMetadata(consignmentId, consignment, fixedDateTime).unsafeRunSync()
    }
    exception.getMessage should equal(s"Incomplete details for user $userId")
  }
}
