package uk.gov.nationalarchives.filexport

import cats.effect.IO
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{EitherValues, FutureOutcome}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class ExportSpec extends AnyFlatSpec with MockitoSugar with Matchers with EitherValues {
  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
}
