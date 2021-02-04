package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import graphql.codegen.GetExportFileMetadata.getFileMetadataForConsignmentExport.GetConsignment
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}
import graphql.codegen.GetFiles.{getFiles => gf}
import graphql.codegen.UpdateExportLocation.{updateExportLocation => uel}
import graphql.codegen.GetOriginalPath.{getOriginalPath => gop}
import graphql.codegen.GetExportFileMetadata.{getFileMetadataForConsignmentExport => gfm}
import sangria.ast.Document
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, SttpBackend}
import uk.gov.nationalarchives.consignmentexport.Config.{Api, Auth, Configuration, EFS, S3}
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

class GraphQlApiSpec extends ExportSpec {
  implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  "the getFiles method" should "returns the correct number of files" in {
    val filesClient = mock[GraphQLClient[gf.Data, gf.Variables]]
    val updateExportClient = mock[GraphQLClient[uel.Data, uel.Variables]]
    val getOriginalPathClient = mock[GraphQLClient[gop.Data, gop.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, filesClient, updateExportClient, getOriginalPathClient, mock[GraphQLClient[gfm.Data, gfm.Variables]])
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.randomUUID()
    val dataFiles = List(UUID.randomUUID(), UUID.randomUUID())


    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[gf.Data](gf.Data(gf.GetFiles(dataFiles)).some, List())
    doAnswer(() => Future(data)).when(filesClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[gf.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    doAnswer(() => Future(GraphQlResponse(gop.Data(gop.GetClientFileMetadata("originalPath".some)).some, List()))).when(getOriginalPathClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[gop.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])

    val files = api.getFiles(config, consignmentId).unsafeRunSync()
    files.map(_.fileId) should equal(dataFiles)
  }

  "the getFiles method" should "throw an exception if there are no files in the response" in {
    val filesClient = mock[GraphQLClient[gf.Data, gf.Variables]]
    val updateExportClient = mock[GraphQLClient[uel.Data, uel.Variables]]
    val getOriginalPathClient = mock[GraphQLClient[gop.Data, gop.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, filesClient, updateExportClient, getOriginalPathClient, mock[GraphQLClient[gfm.Data, gfm.Variables]])
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.randomUUID()

    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[gf.Data](Option.empty[gf.Data], List())
    doAnswer(() => Future(data)).when(filesClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[gf.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])

    val exception = intercept[RuntimeException] {
      api.getFiles(config, consignmentId).unsafeRunSync()
    }
    exception.getMessage should equal(s"No files found for consignment $consignmentId ")
  }

  "the updateExportLocation method" should "return the correct value" in {
    val filesClient = mock[GraphQLClient[gf.Data, gf.Variables]]
    val updateExportClient = mock[GraphQLClient[uel.Data, uel.Variables]]
    val getOriginalPathClient = mock[GraphQLClient[gop.Data, gop.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, filesClient, updateExportClient, getOriginalPathClient, mock[GraphQLClient[gfm.Data, gfm.Variables]])
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.randomUUID()

    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[uel.Data](uel.Data(1.some).some, List())
    doAnswer(() => Future(data)).when(updateExportClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[uel.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])

    val response = api.updateExportLocation(config, consignmentId, s"s3://testbucket/$consignmentId.tar.gz").unsafeRunSync()
    response.isDefined should be(true)
    response.get should equal(1)
  }

  "the updateExportLocation method" should "throw an exception if no data is returned" in {
    val filesClient = mock[GraphQLClient[gf.Data, gf.Variables]]
    val updateExportClient = mock[GraphQLClient[uel.Data, uel.Variables]]
    val getOriginalPathClient = mock[GraphQLClient[gop.Data, gop.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, filesClient, updateExportClient, getOriginalPathClient, mock[GraphQLClient[gfm.Data, gfm.Variables]])
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.randomUUID()

    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[uel.Data](Option.empty[uel.Data], List())
    doAnswer(() => Future(data)).when(updateExportClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[uel.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])

    val exception = intercept[RuntimeException] {
      api.updateExportLocation(config, consignmentId, s"s3://testbucket/$consignmentId.tar.gz").unsafeRunSync()
    }
    exception.getMessage should equal(s"No data returned from the update export call for consignment $consignmentId ")
  }

  "the getFileMetadata method" should "throw an error if the data is not not provided" in {
    val getFileMetadataClient = mock[GraphQLClient[gfm.Data, gfm.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, mock[GraphQLClient[gf.Data, gf.Variables]], mock[GraphQLClient[uel.Data, uel.Variables]], mock[GraphQLClient[gop.Data, gop.Variables]], getFileMetadataClient)
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.fromString("fda625d6-e42c-4d99-83bd-65b3f65f82c9")

    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[gfm.Data](none[gfm.Data], List())
    doAnswer(() => Future(data)).when(getFileMetadataClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[gfm.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val exception = intercept[RuntimeException] {
      api.getFileMetadata(config, consignmentId).unsafeRunSync()
    }
    exception.getMessage should equal("No metadata found for consignment fda625d6-e42c-4d99-83bd-65b3f65f82c9 ")
  }

  "the getFileMetadata method" should "throw an error if the data is provided but the consignment export data is empty" in {
    val getFileMetadataClient = mock[GraphQLClient[gfm.Data, gfm.Variables]]
    val keycloak = mock[KeycloakUtils]
    val api = new GraphQlApi(keycloak, mock[GraphQLClient[gf.Data, gf.Variables]], mock[GraphQLClient[uel.Data, uel.Variables]], mock[GraphQLClient[gop.Data, gop.Variables]], getFileMetadataClient)
    val config = Configuration(S3("", "", ""), Api(""), Auth("authUrl", "clientId", "clientSecret"), EFS(""))
    val consignmentId = UUID.fromString("fda625d6-e42c-4d99-83bd-65b3f65f82c9")

    doAnswer(() => Future(new BearerAccessToken("token"))).when(keycloak).serviceAccountToken[Identity](any[String], any[String])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val data = new GraphQlResponse[gfm.Data](gfm.Data(none[GetConsignment]).some, List())
    doAnswer(() => Future(data)).when(getFileMetadataClient).getResult[Identity](any[BearerAccessToken], any[Document], any[Option[gfm.Variables]])(any[SttpBackend[Identity, Nothing, NothingT]], any[ClassTag[Identity[_]]])
    val exception = intercept[RuntimeException] {
      api.getFileMetadata(config, consignmentId).unsafeRunSync()
    }
    exception.getMessage should equal("No metadata found for consignment fda625d6-e42c-4d99-83bd-65b3f65f82c9 ")
  }
}
