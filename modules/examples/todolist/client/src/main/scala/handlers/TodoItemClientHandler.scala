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
import examples.todolist.client.clients.TodoItemClient
import examples.todolist.protocol._
import examples.todolist.protocol.Protocols._
import freestyle.rpc.protocol.Empty
import freestyle.tagless.logging.LoggingM

class TodoItemClientHandler[F[_]](
    implicit M: Monad[F],
    log: LoggingM[F],
    client: TodoItemRpcService.Client[F])
    extends TodoItemClient.Handler[F] {

  override def reset(): F[Int] =
    for {
      _ <- log.debug(s"Calling to restart todo items data")
      r <- client.reset(Empty)
    } yield r.value

  override def insert(request: TodoItemRequest): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(
        s"Calling to insert todo item with item ${request.item} to list ${request.todoListId}")
      t <- client.insert(request)
    } yield t.msg

  override def retrieve(id: Int): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(s"Calling to get todo item with id: $id")
      r <- client.retrieve(MessageId(id))
    } yield r.msg

  override def list(): F[TodoItemList] =
    for {
      _ <- log.debug(s"Calling to get all todo items")
      r <- client.list(Empty)
    } yield r

  override def update(todoItem: TodoItemMessage): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(
        s"Calling to update todo item ${todoItem.id} with item ${todoItem.id} from list ${todoItem.todoListId} and completed status ${todoItem.completed}")
      r <- client.update(todoItem)
    } yield r.msg

  override def remove(id: Int): F[Int] =
    for {
      _ <- log.debug(s"Calling to delete todo item with id: $id")
      r <- client.destroy(MessageId(id))
    } yield r.value
}
