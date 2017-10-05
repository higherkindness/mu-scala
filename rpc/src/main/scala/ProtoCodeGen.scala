/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle
package rpc
package protocol

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, StandardOpenOption}

import cats.implicits._
import freestyle.rpc.protocol.encoders._
import freestyle.rpc.protocol.model._
import freestyle.rpc.protocol.processors._

import scala.util.{Failure, Success, Try}

object ProtoCodeGen {

  def main(args: Array[String]): Unit = {
    args.toList match {
      case input :: output :: Nil =>
        generate(new File(input), new File(output)) foreach {
          case Some((file, contents)) =>
            Files.write(
              file.toPath,
              contents
                .getBytes(Charset.forName("UTF-8")),
              StandardOpenOption.CREATE
            )
          case None =>
        }
      case _ =>
        throw new IllegalArgumentException(s"Expected 2 $args with input and output directories")
    }
  }

  def generate(input: File, output: File): Seq[Option[(File, String)]] = {
    def allScalaFiles(f: File): List[File] = {
      val children   = f.listFiles
      val scalaFiles = children.filter(f => """.*\.scala$""".r.findFirstIn(f.getName).isDefined)
      (scalaFiles ++ children.filter(_.isDirectory).flatMap(allScalaFiles)).toList
    }
    if (input.isDirectory && output.isDirectory) {
      allScalaFiles(input)
        .map { inputFile =>
          ProtoAnnotationsProcessor[Try]
            .process(inputFile)
            .map {
              case pd if pd.options.nonEmpty && pd.messages.nonEmpty && pd.services.nonEmpty =>
                val protoContents = ProtoEncoder[ProtoDefinitions].encode(pd)
                val outputFile =
                  new File(output.toPath + "/" + inputFile.getName.replaceAll(".scala", ".proto"))
                println(s"""
                           |
                           |Scala File: $inputFile
                           |Proto File: $outputFile
                           |----------------------------------
                           |$protoContents
                           |----------------------------------
                           |""".stripMargin)
                Some((outputFile, protoContents))
              case _ =>
                None
            }
        }
        .map {
          case Success(s) => s
          case Failure(t) => throw t
        }
    } else {
      throw new IllegalArgumentException(s"Expected $input and $output to be directories")
    }
  }

}
