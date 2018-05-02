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
package handlers

import cats.Monad
import cats.Monad.ops._
import examples.todolist.client.clients.TagClient
import examples.todolist.protocol._
import examples.todolist.protocol.Protocols._
import freestyle.rpc.protocol.Empty
import freestyle.tagless.logging.LoggingM

class TagClientHandler[F[_]](
    implicit M: Monad[F],
    log: LoggingM[F],
    client: TagRpcService.Client[F])
    extends TagClient.Handler[F] {

  override def reset(): F[Int] =
    for {
      _ <- log.debug(s"Calling to restart tags data")
      r <- client.reset(Empty)
    } yield r.value

  override def insert(request: TagRequest): F[Option[TagMessage]] =
    for {
      _ <- log.debug(s"Calling to insert tag with name: ${request.name}")
      t <- client.insert(request)
    } yield t.tag

  override def retrieve(id: Int): F[Option[TagMessage]] =
    for {
      _ <- log.debug(s"Calling to get tag with id: $id")
      r <- client.retrieve(MessageId(id))
    } yield r.tag

  override def list(): F[TagList] =
    for {
      _ <- log.debug(s"Calling to get all tags")
      r <- client.list(Empty)
    } yield r

  override def update(tag: TagMessage): F[Option[TagMessage]] =
    for {
      _ <- log.debug(s"Calling to update tag ${tag.id} with name ${tag.name}")
      r <- client.update(tag)
    } yield r.tag

  override def remove(id: Int): F[Int] =
    for {
      _ <- log.debug(s"Calling to delete tag with id: $id")
      r <- client.destroy(MessageId(id))
    } yield r.value
}
