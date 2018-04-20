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

import cats.effect.IO
import examples.todolist.protocol.Protocols.PingPongService
import examples.todolist.runtime.PingPong
import examples.todolist.server.handlers.PingPongServiceHandler
import freestyle.rpc.server._
import freestyle.rpc.server.config.BuildServerFromConfig
import freestyle.rpc.server.{AddService, GrpcConfig, ServerW}
import freestyle.tagless.config.implicits._

trait ServerImplicits extends PingPong {

  implicit val pingPongServiceHandler: PingPongServiceHandler[IO] =
    new PingPongServiceHandler[IO]()

  val gprcConfigs: List[GrpcConfig] =
    List(AddService(PingPongService.bindService[IO]))

  implicit val serverW: ServerW =
    BuildServerFromConfig[IO]("rpc.server.port", gprcConfigs).unsafeRunSync()
}

object implicits extends ServerImplicits
