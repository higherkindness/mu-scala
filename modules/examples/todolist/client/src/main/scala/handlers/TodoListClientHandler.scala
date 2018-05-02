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
import examples.todolist.client.clients.TodoListClient
import examples.todolist.protocol.Protocols._
import examples.todolist.protocol._
import freestyle.rpc.protocol.Empty
import freestyle.tagless.logging.LoggingM

class TodoListClientHandler[F[_]](
    implicit M: Monad[F],
    log: LoggingM[F],
    client: TodoListRpcService.Client[F])
    extends TodoListClient.Handler[F] {

  override def reset(): F[Int] =
    for {
      _ <- log.debug(s"Calling to restart todo list data")
      r <- client.reset(Empty)
    } yield r.value

  override def insert(request: TodoListRequest): F[Option[TodoListMessage]] =
    for {
      _ <- log.debug(
        s"Calling to insert todo list with name: ${request.title} and id: ${request.tagId}")
      t <- client.insert(request)
    } yield t.msg

  override def retrieve(id: Int): F[Option[TodoListMessage]] =
    for {
      _ <- log.debug(s"Calling to get todo list with id: $id")
      r <- client.retrieve(MessageId(id))
    } yield r.msg

  override def list(): F[TodoListList] =
    for {
      _ <- log.debug(s"Calling to get all todo lists")
      r <- client.list(Empty)
    } yield r

  override def update(todoList: TodoListMessage): F[Option[TodoListMessage]] =
    for {
      _ <- log.debug(
        s"Calling to update todo list with title: ${todoList.title}, tagId: ${todoList.tagId} and id: ${todoList.id}")
      r <- client.update(todoList)
    } yield r.msg

  override def remove(id: Int): F[Int] =
    for {
      _ <- log.debug(s"Calling to delete tag with id: $id")
      r <- client.destroy(MessageId(id))
    } yield r.value
}
