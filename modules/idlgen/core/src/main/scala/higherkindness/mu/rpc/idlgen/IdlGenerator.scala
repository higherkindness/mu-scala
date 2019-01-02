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
import java.nio.file.{Files, Paths}

import higherkindness.mu.rpc.idlgen.util._
import higherkindness.mu.rpc.protocol.SerializationType

import scala.collection.JavaConverters._

trait IdlGenerator extends Generator {
  import Model._

  def serializationType: SerializationType
  def outputSubdir: String
  def fileExtension: String

  def inputFiles(files: Set[File]): Seq[File] =
    files.filter(_.getName.endsWith(ScalaFileExtension)).toSeq

  def generateFrom(
      inputFile: File,
      serializationType: String,
      options: String*): Option[(String, Seq[String])] = {
    val inputName = inputFile.getName.replaceAll(ScalaFileExtension, "")
    val definitions =
      ScalaParser.parse(
        Toolbox.parse(Files.readAllLines(Paths.get(inputFile.toURI)).asScala.mkString("\n")),
        inputName)
    generateFrom(definitions).map(output => s"$outputSubdir/$inputName$fileExtension" -> output)
  }

  def generateFrom(rpc: RpcDefinitions): Option[Seq[String]] = {
    val messages = rpc.messages
    val services = filterServices(rpc.services)
    if (messages.nonEmpty || services.nonEmpty)
      Some(generateFrom(rpc.outputName, rpc.outputPackage, rpc.options, messages, services))
    else None
  }

  protected def generateFrom(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]): Seq[String]

  private def filterServices(services: Seq[RpcService]): Seq[RpcService] =
    services
      .filter(_.serializationType == serializationType)
      .filter(_.requests.nonEmpty)
}
