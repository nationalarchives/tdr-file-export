package uk.gov.nationalarchives.consignmentexport

import java.nio.file.Path
import java.util
import java.util.UUID

import cats.effect.IO
import gov.loc.repository.bagit.creator.BagCreator
import gov.loc.repository.bagit.domain.Bag
import gov.loc.repository.bagit.hash.{StandardSupportedAlgorithms, SupportedAlgorithm}
import gov.loc.repository.bagit.verify.BagVerifier
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import uk.gov.nationalarchives.consignmentexport.Utils._

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class Bagit(bagInPlace: (Path, util.Collection[SupportedAlgorithm], Boolean) => Bag,
            validateBag: (Bag, Boolean) => Unit)(implicit val logger: SelfAwareStructuredLogger[IO]) {

  def createBag(consignmentId: UUID, rootLocation: String): IO[Unit] = for {
    bag <- IO.pure(bagInPlace(s"$rootLocation/$consignmentId".toPath, List(StandardSupportedAlgorithms.SHA256: SupportedAlgorithm).asJavaCollection, true))
    _ <- IO.pure(validateBag(bag, true))
    _ <- logger.info(s"Bagit export complete for consignment $consignmentId")
  } yield ()
}

object Bagit {
  //Passing the method value to the class to make unit testing possible as there's no easy way to mock the file writing
  def apply()(implicit logger: SelfAwareStructuredLogger[IO]): Bagit = new Bagit(BagCreator.bagInPlace, new BagVerifier().isComplete)(logger)
}
