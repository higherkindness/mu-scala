/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.idlgen

import higherkindness.mu.rpc.internal.util.FileUtil._
import java.io.File
import org.log4s.getLogger

abstract class GeneratorApplication[T <: Generator](generators: T*) {
  // Code covered by plugin tests
  // $COVERAGE-OFF$

  private[this] val logger = getLogger

  private lazy val generatorsByType = generators.map(gen => gen.idlType -> gen).toMap
  private lazy val idlTypes         = generatorsByType.keySet

  def generateFrom(args: Array[String]): Seq[File] = {
    validate(
      args.length >= 4,
      s"Usage: ${getClass.getName.dropRight(1)} idlType serializationType inputPath outputPath [option1 option2 ...]")

    val idlType           = args(0)
    val serializationType = args(1)

    validate(
      serializationTypes.contains(serializationType),
      s"Unknown Serialization type '$serializationType'. Valid values: ${serializationTypes.mkString(", ")}")

    val inputPath = new File(args(2))
    validate(
      inputPath.exists,
      s"Input path '$inputPath' doesn't exist"
    )
    val outputDir = new File(args(3))
    val options   = args.drop(4)
    generateFrom(idlType, serializationType, inputPath.allFiles.toSet, outputDir, options: _*)
  }

  def generateFrom(
      idlType: String,
      serializationType: String,
      inputFiles: Set[File],
      outputDir: File,
      options: String*): Seq[File] = {
    validate(
      idlTypes.contains(idlType),
      s"Unknown IDL type '$idlType'. Valid values: ${idlTypes.mkString(", ")}")
    generatorsByType(idlType).generateFrom(inputFiles, serializationType, options: _*).map {
      case (inputFile, outputFilePath, output) =>
        val outputFile = new File(outputDir, outputFilePath)
        logger.info(s"$inputFile -> $outputFile")
        Option(outputFile.getParentFile).foreach(_.mkdirs())
        outputFile.write(output)
        outputFile
    }
  }

  private def validate(requirement: Boolean, message: => Any): Unit =
    if (!requirement) {
      System.err.println(message)
      System.exit(1)
    }

  // $COVERAGE-ON$
}
