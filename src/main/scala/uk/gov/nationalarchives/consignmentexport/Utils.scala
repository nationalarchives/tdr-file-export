package uk.gov.nationalarchives.consignmentexport

import java.nio.file.{Path, Paths}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Utils {
  implicit class PathUtils(str: String) {
    def toPath: Path = Paths.get(str)
  }

  implicit class ZonedDatetimeUtils(value: ZonedDateTime) {
    def toFormattedPrecisionString: String = {
      value.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
  }
}
