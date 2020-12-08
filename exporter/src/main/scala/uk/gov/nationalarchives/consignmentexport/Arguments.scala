package uk.gov.nationalarchives.consignmentexport

import java.util.UUID

import cats.implicits._
import com.monovore.decline.Opts

object Arguments {
  case class FileExport(consignmentId: UUID)
  val consignmentId: Opts[UUID] = Opts.option[UUID]("consignmentId", "The id for the consignment")

  val exportOps: Opts[FileExport] =
    Opts.subcommand("export", "Creates a bagit package") {
      consignmentId.map(FileExport)
    }
}
