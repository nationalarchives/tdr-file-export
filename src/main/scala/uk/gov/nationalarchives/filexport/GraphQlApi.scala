package uk.gov.nationalarchives.filexport

import java.util.UUID

import cats.effect.{ContextShift, IO}
import cats.implicits._
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import graphql.codegen.GetFiles.{getFiles => gf}
import graphql.codegen.UpdateExportLocation.{updateExportLocation => uel}
import graphql.codegen.types.UpdateExportLocationInput
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, SttpBackend}
import GraphQlApi._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.filexport.Config.Configuration

import scala.concurrent.Future

class GraphQlApi(keycloak: KeycloakUtils,
                 filesClient: GraphQLClient[gf.Data, gf.Variables],
                 updateExportLocationClient: GraphQLClient[uel.Data, uel.Variables])(implicit val contextShift: ContextShift[IO], val logger: SelfAwareStructuredLogger[IO]) {

  def getFiles(config: Configuration, consignmentId: UUID): IO[gf.Data] = for {
    token <- keycloak.serviceAccountToken(config.auth.clientId, config.auth.clientSecret).toIO
    result <- filesClient.getResult(token, gf.document, gf.Variables(consignmentId).some).toIO
    data <- IO.fromOption(result.data)(new RuntimeException(s"No files found for consignmnt $consignmentId"))
  } yield data

  def updateExportLocation(config: Configuration, consignmentId: UUID, tarPath: String): IO[Option[Int]] = for {
    token <- keycloak.serviceAccountToken(config.auth.clientId, config.auth.clientSecret).toIO
    response <- updateExportLocationClient.getResult(token, uel.document, uel.Variables(UpdateExportLocationInput(consignmentId, tarPath)).some).toIO
    data <- IO.fromOption(response.data)(new RuntimeException(s"No data returned from the update export call for consignment $consignmentId"))
    _ <- logger.info("Export location updated")
  } yield data.updateExportLocation
}

object GraphQlApi {
  def apply(keycloak: KeycloakUtils, filesClient: GraphQLClient[gf.Data, gf.Variables], updateExportLocationClient: GraphQLClient[uel.Data, uel.Variables])
           (implicit contextShift: ContextShift[IO], logger: SelfAwareStructuredLogger[IO]): GraphQlApi = new GraphQlApi(keycloak, filesClient, updateExportLocationClient)(contextShift, logger)

  implicit class FutureUtils[T](f: Future[T])(implicit contextShift: ContextShift[IO]) {
    def toIO: IO[T] = IO.fromFuture(IO(f))
  }

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
}
