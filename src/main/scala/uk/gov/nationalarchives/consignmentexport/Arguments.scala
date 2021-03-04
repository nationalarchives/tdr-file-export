package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.implicits.catsSyntaxTuple2Semigroupal
import com.monovore.decline.Opts

object Arguments {
  //Temporarily make taskToken option to allow for deployment of code without disruption of service
  case class FileExport(consignmentId: UUID, taskToken: Option[String])

  val consignmentId: Opts[UUID] = Opts.option[UUID]("consignmentId", "The id for the consignment")
  val taskToken: Opts[String] = Opts.option[String]("taskToken", "The task token passed to ECS task from the step function")

  val exportOps: Opts[FileExport] =
    Opts.subcommand("export", "Creates a bagit package") {
      (consignmentId, taskToken.withDefault("unset")) mapN {
        (ci, tt) => {
          //Temporarily handle the default taskToken value 'unset' so that it passes in None
          val taskTokenValue = tt match {
            case "unset" => None
            case _ => Some(tt)
          }
          FileExport(ci, taskTokenValue)
        }
      }
    }
}
