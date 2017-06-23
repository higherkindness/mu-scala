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

import cats._
import cats.implicits._
import freestyle.rpc.protocol.converters._
import freestyle.rpc.protocol.model.ProtoDefinitions
import simulacrum.typeclass

import scala.collection.immutable
import scala.meta.Defn.{Class, Trait}
import scala.meta._
import scala.meta.contrib._

object processors {

  @typeclass
  trait ProtoAnnotationsProcessor[M[_]] {
    def process(file: File): M[ProtoDefinitions]
  }

  object ProtoAnnotationsProcessor {
    implicit def defaultProtoAnnotationsProcessor[M[_]](
        implicit ME: MonadError[M, Throwable]): ProtoAnnotationsProcessor[M] =
      new DefaultProtoAnnotationsProcessor[M]()
  }

  class DefaultProtoAnnotationsProcessor[M[_]](
      implicit ME: MonadError[M, Throwable],
      converter: ScalaMetaClass2ProtoMessage)
      extends ProtoAnnotationsProcessor[M] {

    def process(file: File): M[ProtoDefinitions] =
      for {
        source <- sourceOf(file)
        messages      = messageClasses(source)
        services      = serviceClasses(source)
        protoMessages = messages.map(converter.convert)
      } yield ProtoDefinitions(protoMessages.toList, Nil)

    private[this] def sourceOf(file: File): M[Source] =
      ME.catchNonFatal(file.parse[Source].get)

    private[this] def messageClasses(source: Source): immutable.Seq[Class] = source.collect {
      case c: Class if c.hasMod(mod"@message") => c
    }

    private[this] def serviceClasses(source: Source): immutable.Seq[Trait] = source.collect {
      case t: Trait if t.hasMod(mod"@service") => t
    }

  }

}
