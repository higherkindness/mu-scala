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
import examples.todolist.{Tag => LibTag}
import examples.todolist.protocol.Protocols._
import examples.todolist.protocol.common._
import examples.todolist.service.TagService
import freestyle.rpc.protocol.Empty

class TagRpcServiceHandler[F[_]](implicit M: Monad[F], service: TagService[F])
    extends TagRpcService[F] {

  import TagConversions._

  def reset(empty: Empty.type): F[IntMessage] =
    service.reset.map(IntMessage)

  def insert(tagRequest: TagRequest): F[TagResponse] =
    service
      .insert(tagRequest.toTag)
      .map(_.flatMap(_.toRpcTag))
      .map(TagResponse)

  def retrieve(id: IntMessage): F[TagResponse] =
    service
      .retrieve(id.value)
      .map(_.flatMap(_.toRpcTag))
      .map(TagResponse)

  def list(empty: Empty.type): F[TagList] =
    service.list
      .map(_.flatMap(_.toRpcTag))
      .map(TagList)

  def update(tag: Tag): F[TagResponse] =
    service
      .update(tag.toTag)
      .map(_.flatMap(_.toRpcTag))
      .map(TagResponse)

  def destroy(id: IntMessage): F[IntMessage] =
    service
      .destroy(id.value)
      .map(IntMessage)
}

object TagConversions {

  implicit class TagRequestToTag(tr: TagRequest) {
    def toTag: LibTag = LibTag(tr.name)
  }

  implicit class RpcTagToTag(t: Tag) {
    def toTag: LibTag = LibTag(t.name, Option(t.id))
  }

  implicit class TagToRpcTag(t: LibTag) {
    def toRpcTag: Option[Tag] =
      t.id.map(id => Tag(t.name, id))
  }
}
