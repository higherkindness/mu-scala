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
      case input :: output :: Nil if input.endsWith(".scala") =>
        val inputFile = new File(input)

        val processed =
          ProtoAnnotationsProcessor[Try]
            .process(inputFile)
            .map(ProtoEncoder[ProtoDefinitions].encode)

        processed match {
          case Success(protoContents) =>
            val jfile = new File(output)
            //jfile.getParentFile.mkdirs()
            println(protoContents)
            Files.write(
              new File(jfile.toPath + "/" + inputFile.getName.replaceAll(".scala", ".proto")).toPath,
              protoContents.getBytes(Charset.forName("UTF-8")),
              StandardOpenOption.CREATE
              //StandardOpenOption.CREATE_NEW
            )

          case Failure(e) => e.printStackTrace()
        }

    }
  }

}
