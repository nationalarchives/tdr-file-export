package uk.gov.nationalarchives.consignmentexport

import java.nio.file.Path
import java.util
import java.util.UUID

import gov.loc.repository.bagit.domain.{Bag, Version}
import gov.loc.repository.bagit.hash.{StandardSupportedAlgorithms, SupportedAlgorithm}
import org.mockito.ArgumentCaptor

class BagitSpec extends ExportSpec {
  "the createBag method" should "call the bagit methods with the correct values" in {
    val createMock = mock[(Path, util.Collection[SupportedAlgorithm], Boolean) => Bag]
    val verifyMock = mock[(Bag, Boolean) => Unit]

    val createPath: ArgumentCaptor[Path] = ArgumentCaptor.forClass(classOf[Path])
    val createAlgorithms: ArgumentCaptor[util.Collection[SupportedAlgorithm]] = ArgumentCaptor.forClass(classOf[util.Collection[SupportedAlgorithm]])
    val createIncludeHidden: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])
    val verfiyBag: ArgumentCaptor[Bag] = ArgumentCaptor.forClass(classOf[Bag])
    val verifyIncludeHidden: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

    val bag = new Bag(Version.LATEST_BAGIT_VERSION())
    val bagit = new Bagit(createMock, verifyMock)
    val consignmentId = UUID.randomUUID()

    doAnswer(() => bag).when(createMock).apply(createPath.capture(), createAlgorithms.capture(), createIncludeHidden.capture())
    doAnswer(() => ()).when(verifyMock).apply(verfiyBag.capture(), verifyIncludeHidden.capture())

    bagit.createBag(consignmentId, "root").unsafeRunSync()

    createPath.getValue.toString should equal(s"root/$consignmentId")
    createAlgorithms.getValue.toArray()(0) should equal(StandardSupportedAlgorithms.SHA256)
    createIncludeHidden.getValue should equal(true)

    verfiyBag.getValue.getVersion should equal(Version.LATEST_BAGIT_VERSION())
    verifyIncludeHidden.getValue should equal(true)
  }
}
