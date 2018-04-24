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
import examples.todolist.Tag
import examples.todolist.protocol.Protocols.{Tag => RpcTag, _}
import examples.todolist.service.TagService
import freestyle.rpc.protocol.Empty

class TagRpcServiceHandler[F[_]](implicit M: Monad[F], service: TagService[F])
    extends TagRpcService[F] {

  import TagConversions._

  def reset(empty: Empty.type): F[IntClass] =
    service.reset.map(IntClass)

  def insert(tagRequest: TagRequest): F[TagResponse] =
    service
      .insert(tagRequest.toTag)
      .map(v => TagResponse(v.map(_.toRpcTag)))

  def retrieve(id: IntClass): F[TagResponse] =
    service
      .retrieve(id.i)
      .map(v => TagResponse(v.map(_.toRpcTag)))

  def list(empty: Empty.type): F[TagList] =
    service.list
      .map(_.map(_.toRpcTag))
      .map(TagList)

  def update(tag: RpcTag): F[TagResponse] =
    service
      .update(tag.toTag)
      .map(v => TagResponse(v.map(_.toRpcTag)))

  def destroy(id: IntClass): F[IntClass] =
    service
      .destroy(id.i)
      .map(IntClass)
}

object TagConversions {

  implicit class TagRequestToTag(tr: TagRequest) {
    def toTag: Tag = Tag(tr.name)
  }

  implicit class RpcTagToTag(t: RpcTag) {
    def toTag: Tag = Tag(t.name, Option(t.id))
  }

  implicit class TagToRpcTag(t: Tag) {
    def toRpcTag: RpcTag = RpcTag(t.name, t.id.get)
  }
}
