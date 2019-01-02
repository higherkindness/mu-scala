/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.rpc.idlgen.avro

import java.io.File
import scala.annotation.tailrec
import scala.io.Source

/** Adapted from [[avrohugger.filesorter.AvdlFileSorter]].
 * Changes:
 * - Handles imported files not present in the input set
 * - Doesn't require input files to be in canonical form (but returns them as such)
 * - Errors out on unresolved imports instead of going into an infinite loop
 */
object AvdlFileSorter {

  def sortSchemaFiles(inputFiles: Set[File]): Seq[File] = {
    val importsMap = getAllImports(inputFiles)

    @tailrec def addFiles(processedFiles: Seq[File], remainingFiles: List[File]): Seq[File] = {
      remainingFiles match {
        case Nil => processedFiles
        case h :: t =>
          val processedFilesSet = processedFiles.toSet
          if (importsMap(h).forall(processedFilesSet.contains))
            addFiles(processedFiles :+ h, t)
          else
            addFiles(processedFiles, t :+ h)
      }
    }
    addFiles(Seq.empty, importsMap.keys.toList)
  }

  private[this] val importPattern = """\s*import\s+idl\s+"([^"]+)"\s*;\s*""".r

  private[this] def getAllImports(files: Set[File]): Map[File, Set[File]] = {
    files.flatMap { file =>
      val imports = getImports(file)
      getAllImports(imports) ++ Map(file.getCanonicalFile -> imports)
    }.toMap
  }

  private[this] def getImports(file: File): Set[File] = {
    val source = Source.fromFile(file)
    try {
      source
        .getLines()
        .collect {
          case importPattern(currentImport) =>
            val importFile = new File(file.getParentFile, currentImport).getCanonicalFile
            if (!importFile.exists())
              sys.error(s"Unresolved import in $file: $importFile")
            importFile
        }
        .toSet
    } finally source.close()
  }
}
