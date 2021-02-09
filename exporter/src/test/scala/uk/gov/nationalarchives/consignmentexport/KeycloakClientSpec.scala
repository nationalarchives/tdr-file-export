package uk.gov.nationalarchives.consignmentexport

import uk.gov.nationalarchives.consignmentexport.Config.{Api, Auth, Configuration, EFS, S3}

class KeycloakClientSpec extends ExternalServiceSpec {
  private val config = Configuration(
    S3("", "", ""),
    Api(""),
    Auth("http://localhost:9002/auth", "tdr-backend-checks", "client-secret", "tdr"),
    EFS(""),
    "v1")

  "the getUserDetails method" should "return the correct user details" in {
    keycloakGetUser
    val keycloakAdminClient = keycloakCreateAdminClient
    val keycloakClient = new KeycloakClient(keycloakCreateAdminClient, config)

    val userDetails = keycloakClient.getUserDetails(keycloakUserId)
    userDetails.getFirstName should be("FirstName")
    userDetails.getLastName should be("LastName")
  }

  "the getUserDetails method" should "throw a run time exception if no user representation found" in {
    val nonExistentUserId = "nonExistentUserId"
    val keycloakClient = new KeycloakClient(keycloakCreateAdminClient, config)

    val exception = intercept[RuntimeException] {
      keycloakClient.getUserDetails(nonExistentUserId)
    }
    exception.getMessage should equal(s"No valid user found $nonExistentUserId: HTTP 404 Not Found")
  }
}
