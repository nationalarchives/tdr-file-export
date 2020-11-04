package uk.gov.nationalarchives.filexport

import java.util.UUID

import cats.implicits._
import com.monovore.decline.Opts

object Arguments {
  case class FileExport(consignmentId: UUID, includeHiddenFiles: Boolean)
  val consignmentId: Opts[UUID] = Opts.option[UUID]("consignmentId", "The id for the consignment")
  val includeHiddenFiles: Opts[Boolean] = Opts.flag("includeHiddenFiles", "Include hidden files").orFalse

  val exportOps: Opts[FileExport] =
    Opts.subcommand("export", "Creates a bagit package") {
      (consignmentId, includeHiddenFiles).mapN(FileExport)
    }
}
