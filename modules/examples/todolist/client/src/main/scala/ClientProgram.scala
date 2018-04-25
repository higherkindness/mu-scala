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

package examples.todolist.client

import cats.Monad
import cats.Monad.ops._
import examples.todolist.client.clients.{PingPongClient, TagClient}
import examples.todolist.protocol.Protocols._
import org.log4s.getLogger

object ClientProgram {

  val logger = getLogger

  def pongProgram[M[_]: Monad](implicit client: PingPongClient[M]): M[Unit] =
    client.ping()

  def tagProgram[M[_]: Monad](implicit tagClient: TagClient[M]): M[Unit] =
    for {
      _         <- tagClient.reset()
      optionTag <- tagClient.insert(TagRequest("tag"))
    } yield {
      optionTag.foreach { tag =>
        logger.debug(s"Inserted tag with id (${tag.id}) and name (${tag.name})")
        tagClient.destroy(tag.id)
      }
    }
}
