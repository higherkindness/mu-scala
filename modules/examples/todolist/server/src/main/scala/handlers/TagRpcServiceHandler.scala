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
import examples.todolist.protocol._
import examples.todolist.protocol.Protocols._
import examples.todolist.service.TagService
import examples.todolist.Tag
import freestyle.rpc.protocol.Empty

class TagRpcServiceHandler[F[_]](implicit M: Monad[F], service: TagService[F])
    extends TagRpcService[F] {

  import TagConversions._

  def reset(empty: Empty.type): F[MessageId] =
    service.reset.map(MessageId)

  def insert(tagRequest: TagRequest): F[TagResponse] =
    service
      .insert(tagRequest.toTag)
      .map(_.flatMap(_.toTagMessage))
      .map(TagResponse)

  def retrieve(id: MessageId): F[TagResponse] =
    service
      .retrieve(id.value)
      .map(_.flatMap(_.toTagMessage))
      .map(TagResponse)

  def list(empty: Empty.type): F[TagList] =
    service.list
      .map(_.flatMap(_.toTagMessage))
      .map(TagList)

  def update(tag: TagMessage): F[TagResponse] =
    service
      .update(tag.toTag)
      .map(_.flatMap(_.toTagMessage))
      .map(TagResponse)

  def destroy(id: MessageId): F[MessageId] =
    service
      .destroy(id.value)
      .map(MessageId)
}

object TagConversions {

  implicit class TagRequestToTag(tr: TagRequest) {
    def toTag: Tag = Tag(tr.name)
  }

  implicit class TagMessageToTag(t: TagMessage) {
    def toTag: Tag = Tag(t.name, Option(t.id))
  }

  implicit class TagToTagMessage(t: Tag) {
    def toTagMessage: Option[TagMessage] =
      t.id.map(TagMessage(t.name, _))
  }
}
