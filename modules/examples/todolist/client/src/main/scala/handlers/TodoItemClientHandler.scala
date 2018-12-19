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

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Resource, Sync}
import examples.todolist.client.clients.TodoItemClient
import examples.todolist.protocol._
import examples.todolist.protocol.Protocols._
import higherkindness.mu.rpc.protocol.Empty
import freestyle.tagless.logging.LoggingM

class TodoItemClientHandler[F[_]: Sync](client: Resource[F, TodoItemRpcService[F]])(
    implicit log: LoggingM[F])
    extends TodoItemClient[F] {

  override def reset(): F[Int] =
    for {
      _ <- log.debug(s"Calling to restart todo items data")
      r <- client.use(_.reset(Empty))
    } yield r.value

  override def insert(request: TodoItemRequest): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(
        s"Calling to insert todo item with item ${request.item} to list ${request.todoListId}")
      t <- client.use(_.insert(request))
    } yield t.msg

  override def retrieve(id: Int): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(s"Calling to get todo item with id: $id")
      r <- client.use(_.retrieve(MessageId(id)))
    } yield r.msg

  override def list(): F[TodoItemList] =
    for {
      _ <- log.debug(s"Calling to get all todo items")
      r <- client.use(_.list(Empty))
    } yield r

  override def update(todoItem: TodoItemMessage): F[Option[TodoItemMessage]] =
    for {
      _ <- log.debug(
        s"Calling to update todo item ${todoItem.id} with item ${todoItem.id} from list ${todoItem.todoListId} and completed status ${todoItem.completed}")
      r <- client.use(_.update(todoItem))
    } yield r.msg

  override def remove(id: Int): F[Int] =
    for {
      _ <- log.debug(s"Calling to delete todo item with id: $id")
      r <- client.use(_.destroy(MessageId(id)))
    } yield r.value
}
