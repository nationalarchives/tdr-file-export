#!/bin/sh
#mkdir $CONSIGNMENT_ID
#touch $CONSIGNMENT_ID/BagitPackage
#tar -czf $CONSIGNMENT_ID.tar.gz $CONSIGNMENT_ID
#sha256sum $CONSIGNMENT_ID.tar.gz > $CONSIGNMENT_ID.sha256
#aws s3 cp $CONSIGNMENT_ID.tar.gz s3://test-export-files/$CONSIGNMENT_ID.tar.gz
#aws s3 cp $CONSIGNMENT_ID.sha256 s3://test-export-files/$CONSIGNMENT_ID.sha256

package uk.gov.nationalarchives.tdr

import cats.effect.{ContextShift, IO}
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.syntax._
import sangria.ast.Document
import sangria.renderer.QueryRenderer
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.reflect.runtime.universe._
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client.{Response, SttpBackend, asString, basicRequest, _}
import sttp.model.MediaType
import uk.gov.nationalarchives.tdr.GraphQLClient.Error
import uk.gov.nationalarchives.tdr.error.{HttpException, ResponseDecodingException, _}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.reflect.{ClassTag, classTag}

class GraphQLClient[Data, Variables](url: String)(implicit val ec: ExecutionContext, val dataDecoder: Decoder[Data], val variablesEncoder: Encoder[Variables]) {

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  case class GraphqlData(data: Option[Data], errors: List[Error] = Nil)

  def getResult[T[_]](token: BearerAccessToken, document: Document, variables: Option[Variables] = Option.empty)(implicit backend: SttpBackend[T, Nothing, NothingT], tag: ClassTag[T[_]]): Future[GraphQlResponse[Data]] = {
    val queryJson: Json = Json.fromString(QueryRenderer.render(document, QueryRenderer.Compact))
    val variablesJson: Option[Json] = variables.map(_.asJson)
    val fields: immutable.Seq[(String, Json)] = List("query" -> queryJson) ++ variablesJson.map("variables" -> _)
    "".getClass.getCanonicalName
    val body = Json.obj(fields: _*).printWith(GraphQLClient.jsonPrinter)
    val response = basicRequest
      .post(uri"$url")
      .auth.bearer(token.getValue)
      .body(body)
      .contentType(MediaType.ApplicationJson)
      .response(asString)
      .send()

    def process(response: Response[Either[String, String]]) = {
      response.body match {
        case Right(body) => parseBody(body)
        case Left(_) => Future.failed(new HttpException(response))
      }
    }

     //The backend type is either SttpBackend[Future, Nothing, NothingT] for async backends or SttpBackend[Identity, Nothing, NothingT] for sync ones
     //There probably are other choices but these are the only ones we're using and we can always add another match in
    tag match {
      case futureTag if futureTag == classTag[Future[_]] => response.asInstanceOf[Future[Response[Either[String, String]]]].flatMap(process)
      case identityTag if identityTag == classTag[Identity[_]] => process(response.asInstanceOf[Identity[Response[Either[String, String]]]])
    }
  }

  private def parseBody(body: String): Future[GraphQlResponse[Data]] = {
    val parsedBody = parse(body).flatMap(json => json.as[GraphqlData])
    parsedBody match {
      case Left(decodingFailure) => Future.failed(new ResponseDecodingException(body, decodingFailure))
      case Right(graphQlResponseBody) => {
        val response = GraphQlResponse(
          graphQlResponseBody.data,
          graphQlResponseBody.errors.map(e => GraphQlError(e))
        )
        Future.successful(response)
      }
    }
  }

  private def parseIOBody(body: String): IO[GraphQlResponse[Data]] = {
    val parsedBody = parse(body).flatMap(json => json.as[GraphqlData])
    parsedBody match {
      case Left(decodingFailure) => IO.raiseError(new ResponseDecodingException(body, decodingFailure))
      case Right(graphQlResponseBody) =>
        val response = GraphQlResponse(
          graphQlResponseBody.data,
          graphQlResponseBody.errors.map(e => GraphQlError(e))
        )
        IO(response)
    }
  }

  def process(response: Response[Either[String, String]]): IO[GraphQlResponse[Data]] = {
    response.body match {
      case Right(body) => parseIOBody(body)
      case Left(_) => IO.raiseError(new HttpException(response))
    }
  }

  def getIOResult(token: BearerAccessToken, document: Document, variables: Option[Variables] = Option.empty): IO[GraphQlResponse[Data]] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)
    val queryJson: Json = Json.fromString(QueryRenderer.render(document, QueryRenderer.Compact))
    val variablesJson: Option[Json] = variables.map(_.asJson)
    val fields: immutable.Seq[(String, Json)] = List("query" -> queryJson) ++ variablesJson.map("variables" -> _)
    "".getClass.getCanonicalName
    val body = Json.obj(fields: _*).printWith(GraphQLClient.jsonPrinter)
    val request: RequestT[Identity, Either[String, String], Nothing] = basicRequest
      .post(uri"$url")
      .auth.bearer(token.getValue)
      .body(body)
      .contentType(MediaType.ApplicationJson)
      .response(asString)

    for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      response <- backend.send(request)
      decodedResponse <- process(response)
    } yield decodedResponse
  }

}

object GraphQLClient {

  case class Error(message: String, path: List[String], locations: List[Location], extensions: Option[Extensions])

  case class Location(column: Int, line: Int)

  case class Extensions(code: Option[String])

  val jsonPrinter: Printer = Printer
    .noSpaces
    // If any optional values are set to None, ignore them rather than setting them to null. This makes it possible to
    // remove deprecated optional parameters from the API without breaking the clients.
    .copy(dropNullValues = true)
}

case class GraphQlResponse[Data](data: Option[Data], errors: List[GraphQlError])