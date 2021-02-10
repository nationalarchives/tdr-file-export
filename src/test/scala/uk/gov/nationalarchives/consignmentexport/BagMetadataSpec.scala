package uk.gov.nationalarchives.consignmentexport

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import cats.effect.IO
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment
import graphql.codegen.GetConsignmentExport.getConsignmentForExport.GetConsignment.{Series, TransferringBody}
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.Config.{Api, Auth, Configuration, EFS, S3}
import uk.gov.nationalarchives.consignmentexport.Utils._

class BagMetadataSpec extends ExportSpec {

  private val fixedDateTime = ZonedDateTime.now()
  private val userId = UUID.randomUUID()
  private val series = Series(Some("series-code"))
  private val transferringBody = TransferringBody(Some("tb-code"))
  private val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
  private val consignment = GetConsignment(
    userId, Some(fixedDateTime), Some(fixedDateTime), Some(fixedDateTime), Some(series), Some(transferringBody)
  )
  private val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret", "realm"), EFS(""))
  private val userRepresentation = new UserRepresentation()
  userRepresentation.setFirstName("FirstName")
  userRepresentation.setLastName("LastName")

  "the getBagMetadata method" should "return the correct bag metadata for the given consignment id" in {

    val mockKeycloakClient = mock[KeycloakClient]
    val mockGraphQlApi = mock[GraphQlApi]

    doAnswer(() => IO.pure(Some(consignment))).when(mockGraphQlApi).getConsignmentMetadata(config, consignmentId)
    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])

    val bagMetadata = BagMetadata(mockGraphQlApi, mockKeycloakClient).getBagMetadata(consignmentId, config, fixedDateTime).unsafeRunSync()
    bagMetadata.get("Consignment-Series").get(0) should be("series-code")
    bagMetadata.get("Source-Organization").get(0) should be("tb-code")
    bagMetadata.get("Consignment-StartDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Consignment-CompletedDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
    bagMetadata.get("Contact-Name").get(0) should be("FirstName LastName")
    bagMetadata.get("Consignment-ExportDate").get(0) should be(fixedDateTime.toFormattedPrecisionString)
  }

  "the getBagMetadata method" should "throw an exception if a consignment metadata property is missing" in {
    val missingPropertyKey = "Consignment-StartDate"
    val incompleteConsignment = GetConsignment(
      userId, None, Some(fixedDateTime), Some(fixedDateTime), Some(series), Some(transferringBody)
    )
    val mockKeycloakClient = mock[KeycloakClient]
    val mockGraphQlApi = mock[GraphQlApi]

    doAnswer(() => IO.pure(Some(incompleteConsignment))).when(mockGraphQlApi).getConsignmentMetadata(config, consignmentId)
    doAnswer(() => userRepresentation).when(mockKeycloakClient).getUserDetails(any[String])

    val exception = intercept[RuntimeException] {
      BagMetadata(mockGraphQlApi, mockKeycloakClient).getBagMetadata(consignmentId, config, fixedDateTime).unsafeRunSync()
    }
    exception.getMessage should equal(s"Missing consignment metadata property $missingPropertyKey for consignment $consignmentId")
  }

  "the getBagMetadata method" should "throw an exception if no consignment meta data is returned" in {
    val mockKeycloakClient = mock[KeycloakClient]
    val mockGraphQlApi = mock[GraphQlApi]
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret", "realm"), EFS(""))

    doAnswer(() => IO.pure(None)).when(mockGraphQlApi).getConsignmentMetadata(config, consignmentId)

    val exception = intercept[RuntimeException] {
      BagMetadata(mockGraphQlApi, mockKeycloakClient).getBagMetadata(consignmentId, config, fixedDateTime).unsafeRunSync()
    }
    exception.getMessage should equal(s"No consignment metadata found for consignment $consignmentId")
  }

  "the getBagMetadata method" should "throw an exception if incomplete user details are found" in {
    val mockKeycloakClient = mock[KeycloakClient]
    val mockGraphQlApi = mock[GraphQlApi]
    val incompleteUserRepresentation = new UserRepresentation()
    incompleteUserRepresentation.setLastName("LastName")

    doAnswer(() => IO.pure(Some(consignment))).when(mockGraphQlApi).getConsignmentMetadata(config, consignmentId)
    doAnswer(() => incompleteUserRepresentation).when(mockKeycloakClient).getUserDetails(userId.toString)

    val exception = intercept[RuntimeException] {
      BagMetadata(mockGraphQlApi, mockKeycloakClient).getBagMetadata(consignmentId, config, fixedDateTime).unsafeRunSync()
    }
    exception.getMessage should equal(s"Incomplete details for user $userId")
  }
}
