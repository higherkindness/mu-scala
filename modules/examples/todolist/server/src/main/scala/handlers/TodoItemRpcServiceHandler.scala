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
import examples.todolist.service.TodoItemService
import examples.todolist.TodoItem
import examples.todolist.protocol.Protocols._
import examples.todolist.protocol._
import freestyle.rpc.protocol.Empty

class TodoItemRpcServiceHandler[F[_]](implicit M: Monad[F], service: TodoItemService[F])
    extends TodoItemRpcService[F] {

  import TodoItemConversions._

  override def reset(empty: Empty.type): F[MessageId] =
    service.reset.map(MessageId)

  override def insert(item: TodoItemRequest): F[TodoItemResponse] =
    service
      .insert(item.toTodoItem)
      .map(_.flatMap(_.toTodoItemMessage))
      .map(TodoItemResponse)

  override def retrieve(id: MessageId): F[TodoItemResponse] =
    service
      .retrieve(id.value)
      .map(_.flatMap(_.toTodoItemMessage))
      .map(TodoItemResponse)

  override def list(empty: Empty.type): F[TodoItemList] =
    service.list
      .map(_.flatMap(_.toTodoItemMessage))
      .map(TodoItemList)

  override def update(item: TodoItemMessage): F[TodoItemResponse] =
    service
      .update(item.toTodoItem)
      .map(_.flatMap(_.toTodoItemMessage))
      .map(TodoItemResponse)

  override def destroy(id: MessageId): F[MessageId] =
    service
      .destroy(id.value)
      .map(MessageId)

}

object TodoItemConversions {

  implicit class TodoItemRequestToTodoItem(it: TodoItemRequest) {
    def toTodoItem: TodoItem =
      TodoItem(item = it.item, todoListId = it.todoListId.some, completed = false, id = None)
  }

  implicit class TodoItemToTodoItemMessage(it: TodoItem) {
    def toTodoItemMessage: Option[TodoItemMessage] =
      for {
        id         <- it.id
        todoListId <- it.todoListId
      } yield TodoItemMessage(it.item, id, it.completed, todoListId)
  }

  implicit class TodoItemMessageToTodoItem(it: TodoItemMessage) {
    def toTodoItem: TodoItem = TodoItem(it.item, it.id.some, it.completed, it.todoListId.some)
  }

}
