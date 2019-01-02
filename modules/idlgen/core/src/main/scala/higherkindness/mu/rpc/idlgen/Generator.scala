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

package higherkindness.mu.rpc.idlgen

import java.io.File

trait Generator {

  def idlType: String

  def generateFrom(
      files: Set[File],
      serializationType: String,
      options: String*): Seq[(File, String, Seq[String])] =
    inputFiles(files).flatMap(inputFile =>
      generateFrom(inputFile, serializationType, options: _*).map {
        case (outputPath, output) =>
          (inputFile, outputPath, output)
    })

  protected def inputFiles(files: Set[File]): Seq[File]

  protected def generateFrom(
      inputFile: File,
      serializationType: String,
      options: String*): Option[(String, Seq[String])]
}
