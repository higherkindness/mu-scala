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

import scala.meta._

object processors {

  @typeclass
  trait ProtoAnnotationsProcessor[M[_]] {
    def process(file: File): M[ProtoDefinitions]
  }

  object ProtoAnnotationsProcessor {
    implicit def defaultProtoAnnotationsProcessor[M[_]](
        implicit ME: MonadError[M, Throwable],
        C: ScalaMetaSource2ProtoDefinitions): ProtoAnnotationsProcessor[M] =
      new DefaultProtoAnnotationsProcessor[M]()
  }

  class DefaultProtoAnnotationsProcessor[M[_]](
      implicit ME: MonadError[M, Throwable],
      C: ScalaMetaSource2ProtoDefinitions
  ) extends ProtoAnnotationsProcessor[M] {

    def process(file: File): M[ProtoDefinitions] = sourceOf(file) map C.convert

    private[this] def sourceOf(file: File): M[Source] =
      ME.catchNonFatal(file.parse[Source].get)

  }

}
