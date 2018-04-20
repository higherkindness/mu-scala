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

package examples.todolist.client.task

import examples.todolist.client.handlers.PingPongClientHandler
import examples.todolist.client.runtime._
import examples.todolist.protocol.Protocols._
import examples.todolist.runtime._
import monix.eval.Task


trait ClientTaskImplicits extends PingPong with ClientConf {

  implicit val pingPongServiceClient: PingPongService.Client[Task] =
    PingPongService.client[Task](channelFor)

  implicit val pingPongClientHandler: PingPongClientHandler[Task] =
    new PingPongClientHandler[Task]
}

object implicits extends ClientTaskImplicits