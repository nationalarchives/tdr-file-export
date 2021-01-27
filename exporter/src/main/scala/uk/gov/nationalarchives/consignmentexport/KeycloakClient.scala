package uk.gov.nationalarchives.consignmentexport

import org.keycloak.OAuth2Constants
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, SttpBackend}
import org.keycloak.admin.client.resource.{RealmResource, UsersResource}
import org.keycloak.admin.client.{Keycloak, KeycloakBuilder}
import org.keycloak.representations.idm.UserRepresentation
import uk.gov.nationalarchives.consignmentexport.Config.Configuration

import scala.language.postfixOps

class KeycloakClient(keycloakAdminClient: Keycloak, config: Configuration) {
  private def realmResource(client: Keycloak): RealmResource = client.realm(config.auth.realm)
  private def userResource(realm: RealmResource): UsersResource = realm.users()

  def getUserDetails(userId: String): UserRepresentation = {
    val realm = realmResource(keycloakAdminClient)
    val user = userResource(realm)
    user.get(userId).toRepresentation
  }
}

object KeycloakClient {
  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  def apply(config: Configuration): KeycloakClient = {
    val keycloakAdminClient = KeycloakBuilder.builder()
      .serverUrl(config.auth.url)
      .realm(config.auth.realm)
      .clientId(config.auth.clientId)
      .clientSecret(config.auth.clientSecret)
      .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
      .build()
    new KeycloakClient(keycloakAdminClient, config)
  }
}
