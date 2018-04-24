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

package examples.todolist
package protocol

import freestyle.rpc.protocol._

trait PingPongProtocol {

  /**
   * Pong response with current timestamp
   *
   * @param time Current timestamp.
   */
  @message
  case class Pong(time: Long = System.currentTimeMillis() / 1000L)

  @service
  trait PingPongService[F[_]] {

    /**
     * A simple ping-pong rpc.
     *
     * @param empty
     * @return Pong response with current timestamp.
     */
    @rpc(Protobuf)
    def ping(empty: Empty.type): F[Pong]

  }
}

trait TagProtocol {

  import monix.reactive.Observable

  @message
  case class TagRequest(name: String)

  @message
  case class Tag(name: String, id: Int)

  @service
  trait TagRpcService[F[_]] {

    @rpc(Protobuf)
    def reset(empty: Empty.type): F[Int]

    @rpc(Protobuf)
    def insert(tagRequest: TagRequest): F[Option[Tag]]

    @rpc(Protobuf)
    def retrieve(id: Int): F[Option[Tag]]

    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def list(empty: Empty.type): Observable[Tag]

    @rpc(Protobuf)
    def update(tag: Tag): F[Option[Tag]]

    @rpc(Protobuf)
    def destroy(id: Int): F[Int]
  }
}

@outputPackage("todolist")
@outputName("TodoListService")
@option("java_multiple_files", true)
@option("java_outer_classname", "TodoListProtoc")
@option("objc_class_prefix", "TL")
object Protocols extends PingPongProtocol with TagProtocol
