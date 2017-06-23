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
import freestyle.rpc.protocol.converters._
import freestyle.rpc.protocol.encoders.ProtoEncoder
import freestyle.rpc.protocol.model.ProtoMessage

import scala.collection.immutable
import scala.meta.Mod.Annot
import scala.meta._
import scala.util.Try
import scala.meta.Defn.{Class, Trait}
import scala.meta.contrib._

object ProtoCodeGen {

  def main(args: Array[String]): Unit = {
    args.toList match {
      case input :: output :: Nil if input.endsWith(".scala") =>
        val jfile = new File(output)
        jfile.getParentFile.mkdirs()
        // Do scala.meta code generation here.

        println(ProtoAnnotationProcessor[Try].processAnnotations(new File(input)))
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

  class ProtoAnnotationProcessor[M[_]](
      implicit ME: MonadError[M, Throwable],
      converter: ScalaMetaClass2ProtoMessage,
      encoder: ProtoEncoder[ProtoMessage]) {

    def processAnnotations(file: File) =
      for {
        source <- sourceOf(file)
        messages      = messageClasses(source)
        services      = serviceClasses(source)
        protoMessages = messages.map(encodeMessage)
      } yield protoMessages

    private[this] def sourceOf(file: File): M[Source] =
      ME.catchNonFatal(file.parse[Source].get)

    private[this] def messageClasses(source: Source): immutable.Seq[Class] = source.collect {
      case c: Class if c.hasMod(mod"@message") => c
    }

    private[this] def serviceClasses(source: Source): immutable.Seq[Trait] = source.collect {
      case t: Trait if t.hasMod(mod"@service") => t
    }

    private[this] def encodeMessage(c: Class): String =
      encoder.encode(converter.convert(c))

  }

  object ProtoAnnotationProcessor {
    def apply[M[_]](
        implicit ME: MonadError[M, Throwable],
        PC: ScalaMetaClass2ProtoMessage,
        EN: ProtoEncoder[ProtoMessage]) =
      new ProtoAnnotationProcessor[M]()
  }

}
