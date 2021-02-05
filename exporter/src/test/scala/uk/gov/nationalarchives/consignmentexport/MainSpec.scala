package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Files
import java.util.UUID

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.apache.commons.codec.digest.DigestUtils
import uk.gov.nationalarchives.consignmentexport.Utils.PathUtils

import scala.io.Source
import scala.sys.process._
import scala.jdk.CollectionConverters._

class MainSpec extends ExternalServiceSpec {

  "the export job" should "export the correct tar and checksum file" in {
    setUpValidExternalServices()

    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")

    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    val objects = outputBucketObjects().map(_.key())

    objects.size should equal(2)
    objects.head should equal(s"$consignmentId.tar.gz")
    objects.last should equal(s"$consignmentId.tar.gz.sha256")
  }

  "the export job" should "export a valid tar and checksum file" in {
    setUpValidExternalServices()

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

    val expectedChecksum = DigestUtils.sha256Hex(Files.readAllBytes(s"$path/result.tar.gz".toPath))

    checksum should equal(expectedChecksum)
    source.close()
  }

  "the export job" should "update the export location in the api" in {
    setUpValidExternalServices()

    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()

    val exportLocationEvent: Option[ServeEvent] = wiremockGraphqlServer.getAllServeEvents.asScala
      .find(p => p.getRequest.getBodyAsString.contains("mutation updateExportLocation"))

    exportLocationEvent.isDefined should be(true)
    exportLocationEvent.get.getRequest.getBodyAsString.contains("\"consignmentId\":\"50df01e6-2e5e-4269-97e7-531a755b417d\"") should be(true)
  }

  "the export job" should "throw an error if the api returns no files for the consignment" in {
    graphQlGetDifferentConsignmentMetadata
    keycloakGetUser
    graphqlGetEmptyFiles
    graphqlGetFileMetadata
    val consignmentId = "6794231c-39fe-41e0-a498-b6a077563282"

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId)).unsafeRunSync()
    }
    ex.getMessage should equal(s"Consignment API returned no files for consignment $consignmentId")
  }


  "the export job" should "throw an error if the file metadata is missing" in {
    graphqlGetFiles
    val consignmentId = "50df01e6-2e5e-4269-97e7-531a755b417d"
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId)).unsafeRunSync()
    }
    ex.getMessage should equal(s"No metadata found for consignment $consignmentId ")
  }

  "the export job" should "throw an error if no consignment metadata found" in {
    graphqlGetFiles
    keycloakGetUser
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    }

    ex.getMessage should equal(s"No consignment metadata found for consignment $consignmentId")
  }

  "the export job" should "throw an error if no valid Keycloak user found" in {
    graphqlGetFiles
    graphQlGetConsignmentMetadata
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    }

    ex.getMessage should equal(s"No valid user found $keycloakUserId: HTTP 404 Not Found")
  }

  "the export job" should "throw an error if an incomplete Keycloak user details found" in {
    graphqlGetFiles
    graphQlGetConsignmentMetadata
    keycloakGetIncompleteUser
    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()
    }

    ex.getMessage should equal(s"Incomplete details for user $keycloakUserId")
  }

  private def setUpValidExternalServices() = {
    graphQlGetConsignmentMetadata
    keycloakGetUser
    graphqlGetFiles,
    graphqlGetFileMetadata
  }
}
