package uk.gov.nationalarchives.consignmentexport

import cats.effect.{ContextShift, IO}
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.{RealmResource, UserResource, UsersResource}
import org.keycloak.representations.idm.UserRepresentation
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, SttpBackend}
import uk.gov.nationalarchives.consignmentexport.Config.{Api, Auth, Configuration, EFS, S3}

import scala.concurrent.ExecutionContextExecutor

class KeycloakClientSpec extends ExportSpec {
  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)

  "the getUserDetails method" should "return the correct user details" in {
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret", "realm"), EFS(""))
    val mockKeycloak = mock[Keycloak]
    val mockRealmResource = mock[RealmResource]
    val mockUsersResource = mock[UsersResource]
    val mockUserResource = mock[UserResource]
    val userRepresentation = new UserRepresentation()
    userRepresentation.setFirstName("FirstName")
    userRepresentation.setLastName("LastName")

    doAnswer(() => mockRealmResource).when(mockKeycloak).realm("realm")
    doAnswer(() => mockUsersResource).when(mockRealmResource).users()
    doAnswer(() => mockUserResource).when(mockUsersResource).get("userId")
    doAnswer(() => userRepresentation).when(mockUserResource).toRepresentation

    val keycloakClient = new KeycloakClient(mockKeycloak, config)
    val userDetails = keycloakClient.getUserDetails("userId")
    userDetails.getFirstName should be("FirstName")
    userDetails.getLastName should be("LastName")
  }

  "the getUserDetails method" should "throw a run time exception if no user representation found" in {
    val userId = "userId"
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret", "realm"), EFS(""))
    val mockKeycloak = mock[Keycloak]
    val mockRealmResource = mock[RealmResource]
    val mockUsersResource = mock[UsersResource]
    val mockRunTimeException = mock[RuntimeException]

    doAnswer(() => mockRealmResource).when(mockKeycloak).realm("realm")
    doAnswer(() => mockUsersResource).when(mockRealmResource).users()
    doAnswer(() => throw mockRunTimeException).when(mockUsersResource).get(userId)
    doAnswer(() => "error message").when(mockRunTimeException).getMessage

    val keycloakClient = new KeycloakClient(mockKeycloak, config)

    val exception = intercept[RuntimeException] {
      val userDetails = keycloakClient.getUserDetails("userId")
    }
    exception.getMessage should equal(s"No valid user found ${userId}: error message")
  }
}
