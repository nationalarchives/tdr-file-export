package uk.gov.nationalarchives.consignmentexport

import java.nio.file.{Path, Paths}

object Utils {
  implicit class PathUtils(str: String) {
    def toPath: Path = Paths.get(str)
  }
}
