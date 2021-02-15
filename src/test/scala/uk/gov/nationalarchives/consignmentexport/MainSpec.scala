package uk.gov.nationalarchives.consignmentexport

import java.io.File
import java.nio.file.Files
import java.util.UUID

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.apache.commons.codec.digest.DigestUtils
import uk.gov.nationalarchives.consignmentexport.Utils.PathUtils

import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.sys.process._

class MainSpec extends ExternalServiceSpec {

  "the export job" should "export the correct tar and checksum file" in {
    setUpValidExternalServices()

    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()

    checkStepFunctionSuccessNotCalled()
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

    checkStepFunctionSuccessNotCalled()

    val downloadDirectory = s"$scratchDirectory/download"
    new File(s"$downloadDirectory").mkdirs()
    getObject(s"$consignmentId.tar.gz", s"$downloadDirectory/result.tar.gz".toPath)
    getObject(s"$consignmentId.tar.gz.sha256", s"$downloadDirectory/result.tar.gz.sha256".toPath)

    val exitCode = Seq("sh", "-c", s"tar -tf $downloadDirectory/result.tar.gz > /dev/null").!
    exitCode should equal(0)

    val source = Source.fromFile(new File(s"$downloadDirectory/result.tar.gz.sha256"))
    val checksum = source.getLines().toList.head.split(" ").head

    val expectedChecksum = DigestUtils.sha256Hex(Files.readAllBytes(s"$downloadDirectory/result.tar.gz".toPath))

    checksum should equal(expectedChecksum)
    source.close()
  }

  "the export job" should "update the export location in the api" in {
    setUpValidExternalServices()

    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString)).unsafeRunSync()

    checkStepFunctionSuccessNotCalled()

    val exportLocationEvent: Option[ServeEvent] = wiremockGraphqlServer.getAllServeEvents.asScala
      .find(p => p.getRequest.getBodyAsString.contains("mutation updateExportLocation"))

    exportLocationEvent.isDefined should be(true)
    exportLocationEvent.get.getRequest.getBodyAsString.contains("\"consignmentId\":\"50df01e6-2e5e-4269-97e7-531a755b417d\"") should be(true)
  }

  "the export job" should "throw an error if the api returns no files for the consignment" in {
    graphQlGetDifferentConsignmentMetadata
    keycloakGetUser
    graphqlGetEmptyFiles
    val consignmentId = "6794231c-39fe-41e0-a498-b6a077563282"

    val ex = intercept[Exception] {
      Main.run(List("export", "--consignmentId", consignmentId)).unsafeRunSync()
    }
    ex.getMessage should equal(s"Consignment API returned no files for consignment $consignmentId")
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
      Main.run(List("export", "--consignmentId", consignmentId.toString, "--taskToken", "taskToken")).unsafeRunSync()
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

  "the export job" should "should publish the step function success token if task token argument provided" in {
    setUpValidExternalServices()

    val consignmentId = UUID.fromString("50df01e6-2e5e-4269-97e7-531a755b417d")
    val taskTokenValue = "taskToken1234"
    putFile(s"$consignmentId/7b19b272-d4d1-4d77-bf25-511dc6489d12")
    Main.run(List("export", "--consignmentId", consignmentId.toString, "--taskToken", taskTokenValue)).unsafeRunSync()

    wiremockSfnServer.getAllServeEvents.size() should be(1)
    val eventRequestBody = wiremockSfnServer.getAllServeEvents.get(0).getRequest.getBodyAsString
    eventRequestBody.contains(taskTokenValue) should be(true)

    //check rest of process was completed successfully
    outputBucketObjects().size should equal(2)
  }

  private def setUpValidExternalServices() = {
    graphQlGetConsignmentMetadata
    keycloakGetUser
    graphqlGetFiles
  }

  private def checkStepFunctionSuccessNotCalled() = {
    //If no taskToken step function success call should not be called
    wiremockSfnServer.getAllServeEvents.size() should be(0)
  }

}
