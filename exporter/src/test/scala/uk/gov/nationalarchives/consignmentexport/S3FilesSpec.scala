package uk.gov.nationalarchives.consignmentexport

import java.nio.file.Path
import java.util.UUID

import cats.effect.IO
import org.mockito.ArgumentCaptor
import software.amazon.awssdk.services.s3.model.{GetObjectResponse, PutObjectResponse}
import uk.gov.nationalarchives.aws.utils.S3Utils
import uk.gov.nationalarchives.consignmentexport.GraphQlApi.FileIdWithPath

class S3FilesSpec extends ExportSpec {

  "the downloadFiles method" should "call the library method with the correct arguments" in {
    val s3Utils = mock[S3Utils]
    val bucketCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val keyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val pathCaptor: ArgumentCaptor[Option[Path]] = ArgumentCaptor.forClass(classOf[Option[Path]])
    val mockResponse = IO.pure(GetObjectResponse.builder.build())
    doAnswer(() => mockResponse).when(s3Utils).downloadFiles(bucketCaptor.capture(), keyCaptor.capture(), pathCaptor.capture())

    val consignmentId = UUID.randomUUID()
    val fileId = UUID.randomUUID()

    S3Files(s3Utils).downloadFiles(List(FileIdWithPath(fileId, "originalPath")), "testbucket", consignmentId, "root").unsafeRunSync()

    bucketCaptor.getValue should equal("testbucket")
    keyCaptor.getValue should equal(s"/$consignmentId/$fileId")
    pathCaptor.getValue.isDefined should equal(true)
    pathCaptor.getValue.get.toString should equal(s"root/$consignmentId/originalPath")
  }

  "the uploadFiles method" should "call the library method with the correct arguments" in {
    val s3Utils = mock[S3Utils]
    val bucketCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val keyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val pathCaptor: ArgumentCaptor[Path] = ArgumentCaptor.forClass(classOf[Path])
    val mockResponse = IO.pure(PutObjectResponse.builder.build())
    doAnswer(() => mockResponse).when(s3Utils).upload(bucketCaptor.capture(), keyCaptor.capture(), pathCaptor.capture())

    val consignmentId = UUID.randomUUID()
    S3Files(s3Utils).uploadFiles("testbucket", consignmentId, "fakepath").unsafeRunSync()

    bucketCaptor.getAllValues.forEach(b => b should equal("testbucket"))
    val keyValues = keyCaptor.getAllValues
    keyValues.get(0)  should equal(s"$consignmentId.tar.gz")
    keyValues.get(1)  should equal(s"$consignmentId.tar.gz.sha256")
    val pathValues = pathCaptor.getAllValues
    pathValues.get(0).toString  should equal("fakepath")
    pathValues.get(1).toString  should equal("fakepath.sha256")
  }
}
