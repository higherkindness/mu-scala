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

package examples.todolist.server
package handlers

import cats.Monad
import cats.Monad.ops._
import cats.syntax.option._
import examples.todolist.protocol._
import examples.todolist.protocol.Protocols._
import examples.todolist.service.TodoListService
import examples.todolist.TodoList
import freestyle.rpc.protocol.Empty

class TodoListRpcServiceHandler[F[_]](implicit M: Monad[F], service: TodoListService[F])
    extends TodoListRpcService[F] {

  import TodoListConversions._

  override def reset(empty: Empty.type): F[MessageId] =
    service.reset.map(MessageId)

  override def insert(item: TodoListRequest): F[TodoListResponse] =
    service
      .insert(item.toTodoList)
      .map(_.toTodoList)

  override def retrieve(id: MessageId): F[TodoListResponse] =
    service
      .retrieve(id.value)
      .map(_.toTodoList)

  override def list(empty: Empty.type): F[TodoListList] =
    service.list
      .map(_.flatMap(_.toTodoListMessage))
      .map(TodoListList)

  override def update(item: TodoListMessage): F[TodoListResponse] =
    service
      .update(item.toTodoList)
      .map(_.toTodoList)

  override def destroy(id: MessageId): F[MessageId] =
    service
      .destroy(id.value)
      .map(MessageId)
}

object TodoListConversions {

  implicit class TodoListRequestToTodoList(tr: TodoListRequest) {
    def toTodoList: TodoList = TodoList(tr.title, tr.tagId.some, None)
  }

  implicit class TodoListToTodoListMessage(tl: TodoList) {
    def toTodoListMessage: Option[TodoListMessage] =
      for {
        id    <- tl.id
        tagid <- tl.tagId
      } yield TodoListMessage(tl.title, id, tagid)
  }

  implicit class TodoListMessageToTodoList(tm: TodoListMessage) {
    def toTodoList: TodoList = TodoList(tm.title, tm.tagId.some, tm.id.some)
  }

  implicit class OptionTodoListTodoListResponse(ol: Option[TodoList]) {

    def toTodoList: TodoListResponse =
      TodoListResponse(ol.flatMap(_.toTodoListMessage))
  }
}
