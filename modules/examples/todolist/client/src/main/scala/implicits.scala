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

import cats.effect.{ContextShift, IO, Resource, Timer}
import examples.todolist.client.handlers._
import examples.todolist.protocol.Protocols._
import examples.todolist.runtime.CommonRuntime
import freestyle.tagless.loggingJVM.log4s.implicits._
import higherkindness.mu.rpc.ChannelFor
import higherkindness.mu.rpc.client.config.ConfigForAddress

trait ClientImplicits extends CommonRuntime {

  implicit val timer: Timer[IO]     = IO.timer(EC)
  implicit val cs: ContextShift[IO] = IO.contextShift(EC)

  val channelFor: ChannelFor =
    ConfigForAddress[IO]("rpc.client.host", "rpc.client.port").unsafeRunSync()

  val pingPongServiceClient: Resource[IO, PingPongService[IO]] =
    PingPongService.client[IO](channelFor)

  implicit val pingPongClientHandler: PingPongClientHandler[IO] =
    new PingPongClientHandler[IO](pingPongServiceClient)

  val tagRpcServiceClient: Resource[IO, TagRpcService[IO]] =
    TagRpcService.client[IO](channelFor)

  implicit val tagClientHandler: TagClientHandler[IO] =
    new TagClientHandler[IO](tagRpcServiceClient)

  val todoListRpcServiceClient: Resource[IO, TodoListRpcService[IO]] =
    TodoListRpcService.client[IO](channelFor)

  implicit val todoListClientHandler: TodoListClientHandler[IO] =
    new TodoListClientHandler[IO](todoListRpcServiceClient)

  val todoItemRpcServiceClient: Resource[IO, TodoItemRpcService[IO]] =
    TodoItemRpcService.client[IO](channelFor)

  implicit val todoItemClientHandler: TodoItemClientHandler[IO] =
    new TodoItemClientHandler[IO](todoItemRpcServiceClient)

}

object implicits extends ClientImplicits
