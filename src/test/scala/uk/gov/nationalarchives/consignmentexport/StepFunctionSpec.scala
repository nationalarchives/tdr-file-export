package uk.gov.nationalarchives.consignmentexport

import cats.effect.IO
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.mockito.ArgumentCaptor
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessResponse
import uk.gov.nationalarchives.aws.utils.StepFunctionUtils
import uk.gov.nationalarchives.consignmentexport.StepFunction.ExportOutput

class StepFunctionSpec extends ExportSpec {

  "the publishSuccess method" should "call the library method with the correct arguments" in {
    val sfnUtils = mock[StepFunctionUtils]
    val taskTokenCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val exportOutputCaptor: ArgumentCaptor[Json] = ArgumentCaptor.forClass(classOf[Json])
    val mockResponse = IO.pure(SendTaskSuccessResponse.builder.build)

    doAnswer(() => mockResponse).when(sfnUtils).sendTaskSuccessRequest(taskTokenCaptor.capture(), exportOutputCaptor.capture())

    val taskToken = "taskToken1234"
    val exportOutput = ExportOutput("userId", "consignmentReference", "tb-code")

    StepFunction(sfnUtils).publishSuccess(Some(taskToken), exportOutput).unsafeRunSync()
    taskTokenCaptor.getValue should equal(taskToken)
    exportOutputCaptor.getValue should equal(exportOutput.asJson)
  }
}
