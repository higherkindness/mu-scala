/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen

import higherkindness.mu.rpc.internal.util.FileUtil._
import java.io.File
import org.log4s.getLogger
import higherkindness.mu.rpc.srcgen.Model.{IdlType, SerializationType}

class GeneratorApplication[T <: Generator](generators: T*) {
  // Code covered by plugin tests
  // $COVERAGE-OFF$

  private[this] val logger = getLogger

  private lazy val generatorsByType = generators.map(gen => gen.idlType -> gen).toMap
  private lazy val idlTypes         = generatorsByType.keySet

  def generateFrom(
      idlType: IdlType,
      serializationType: SerializationType,
      inputFiles: Set[File],
      outputDir: File
  ): Seq[File] =
    if (idlTypes.contains(idlType))
      generatorsByType(idlType).generateFrom(inputFiles, serializationType).map {
        case (inputFile, outputFilePath, output) =>
          val outputFile = new File(outputDir, outputFilePath)
          logger.info(s"$inputFile -> $outputFile")
          Option(outputFile.getParentFile).foreach(_.mkdirs())
          outputFile.write(output)
          outputFile
      }
    else {
      System.out.println(
        s"Unknown IDL type '$idlType', skipping code generation in this module. " +
          s"Valid values: ${idlTypes.mkString(", ")}"
      )
      Seq.empty[File]
    }

  // $COVERAGE-ON$
}
