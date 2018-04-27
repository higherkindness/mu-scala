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

import cats.implicits._
import cats.Monad
import examples.todolist.client.clients._
import examples.todolist.protocol.Protocols._
import org.log4s.{getLogger, Logger}

object ClientProgram {

  val logger: Logger = getLogger

  def pongProgram[M[_]: Monad](implicit client: PingPongClient[M]): M[Unit] =
    client.ping()

  def exampleProgram[M[_]: Monad](
      implicit tagClient: TagClient[M],
      todoListClient: TodoListClient[M],
      todoItemClient: TodoItemClient[M]): M[Unit] =
    for {
      _      <- tagClient.reset()
      _      <- todoListClient.reset()
      _      <- todoItemClient.reset()
      optTag <- tagClient.insert(TagRequest("tag"))
      optList <- optTag
        .map(tg => todoListClient.insert(TodoListRequest("list", tg.id)))
        .sequence
        .map(_.flatten)
      _ <- optList
        .map(tl => todoItemClient.insert(TodoItemRequest("item", tl.id)))
        .sequence
        .map(_.flatten)
    } yield logger.debug("Example program executed properly")
}
