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
import java.nio.file.Files

import cats._
import cats.implicits._

import scala.collection.immutable
import scala.meta.Mod.Annot
import scala.meta._
import scala.util.Try
import scala.meta.Defn.{Class, Object, Trait}

object ProtoAnnotationProcessor {

  def main(args: Array[String]): Unit = {
    args.toList match {
      case input :: output :: Nil if input.endsWith(".scala") =>
        val jfile = new File(output)
        jfile.getParentFile.mkdirs()
        // Do scala.meta code generation here.
        println(annotations[Try](new File(input)))
//        Files.write(
//          jfile.toPath,
//          source"""package mycodegen
//                   object Generated {
//                     val msg = "Hello world!"
//                   }
//                   """.syntax.getBytes("UTF-8")
//        )
    }
  }

  def annotations[M[_]](file: File)(implicit ME: MonadError[M, Throwable]): M[List[Tree]] =
    for {
      tree <- ME.catchNonFatal(file.parse[Source].get)
      annotations = tree.collect {
        case msg: Class  => msg
        case svcs: Trait => svcs
      }
    } yield annotations

}
