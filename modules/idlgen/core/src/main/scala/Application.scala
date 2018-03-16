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

package freestyle.rpc.idlgen

import freestyle.rpc.internal.util.FileUtil._
import java.io.File
import scala.meta._

object Application {
  // Code covered by plugin tests
  // $COVERAGE-OFF$

  val ScalaFileExtension = ".scala"
  val Separator: String  = "-" * 80

  def main(args: Array[String]): Unit = {
    require(args.length >= 2, s"Usage: ${getClass.getName} inputPath outputPath")
    val inputPath  = new File(args(0)).requireExisting
    val outputPath = new File(args(1)).requireExisting
    inputPath.allMatching(_.getName.endsWith(ScalaFileExtension)).foreach { inputFile =>
      val outputName = inputFile.getName.replaceAll(ScalaFileExtension, "")
      Generator.generateFrom(Parser.parse(inputFile.parse[Source].get, outputName)).foreach {
        case (generator, output) =>
          val outputDir  = new File(outputPath, generator.outputSubdir)
          val outputFile = new File(outputDir, outputName + generator.fileExtension)
          outputDir.mkdir()
          outputFile.write(output)
          println(Separator)
          println(s"$inputFile -> $outputFile")
          println(Separator)
          output.foreach(println)
      }
    }
  }
  // $COVERAGE-ON$
}
