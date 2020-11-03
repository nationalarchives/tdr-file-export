package uk.gov.nationalarchives.filexport

import cats.effect.{Blocker, ContextShift, IO}
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect.syntax._
import pureconfig.generic.auto._

object Config {

  case class S3(endpoint: String, cleanBucket: String, outputBucket: String)
  case class Api(url: String)
  case class Auth(url: String, clientId: String, clientSecret: String)
  case class EFS(rootLocation: String)
  case class Configuration(s3: S3, api: Api, auth: Auth, efs: EFS)

  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  def config()(implicit contextShift: ContextShift[IO]): IO[Configuration] = Blocker[IO].use(ConfigSource.default.loadF[IO, Configuration])

}
