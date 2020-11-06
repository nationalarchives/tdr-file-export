package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.util.UUID

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import uk.gov.nationalarchives.consignmentexport.Utils.PathUtils

import scala.io.Source
import scala.sys.process._
import scala.jdk.CollectionConverters._

class MainSpec extends ExternalServiceSpec {

  "the export job" should "export the correct tar and checksum file" in {
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    val objects = outputBucketObjects().map(_.key())

    objects.size should equal(2)
    objects.head should equal(s"$consignmentId.tar.gz")
    objects.last should equal(s"$consignmentId.tar.gz.sha256")

    wiremockGraphqlServer.getAllServeEvents.asScala.map(_.getRequest.getBodyAsString).foreach(println)
  }

  "the export job" should "export a valid tar and checksum file" in {
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    val path = s"src/test/resources/testfiles/$consignmentId"
    getObject(s"$consignmentId.tar.gz", s"$path/result.tar.gz".toPath)
    getObject(s"$consignmentId.tar.gz.sha256", s"$path/result.tar.gz.sha256".toPath)

    val exitCode = Seq("sh", "-c", s"tar -tf $path/result.tar.gz > /dev/null").!
    exitCode should equal(0)

    val source = Source.fromFile(new File(s"$path/result.tar.gz.sha256"))
    val checksum = source.getLines().toList.head.split(" ").head

    //We can't check the file checksum because tar uses a timestamp to gzip so it's different every time
    checksum.matches("[a-z0-9]{64}") should equal(true)
    source.close()
  }

  "the export job" should "update the export location in the api" in {
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()

    val exportLocationEvent: Option[ServeEvent] = wiremockGraphqlServer.getAllServeEvents.asScala
      .find(p => p.getRequest.getBodyAsString.contains("mutation updateExportLocation"))

    exportLocationEvent.isDefined should be(true)
    exportLocationEvent.get.getRequest.getBodyAsString.contains("\"consignmentId\":\"50df01e6-2e5e-4269-97e7-531a755b417d\"") should be(true)
  }
}
