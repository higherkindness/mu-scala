/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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
import cats.instances.list._
import cats.syntax.traverse._
import examples.todolist.protocol.Protocols._
import examples.todolist.server.implicits._
import higherkindness.mu.rpc.config.server.BuildServerFromConfig
import higherkindness.mu.rpc.server.{AddService, GrpcConfig, GrpcServer}

object ServerApp {

  def main(args: Array[String]): Unit = {

    val grpcConfigs: IO[List[GrpcConfig]] =
      List(
        PingPongService.bindService[IO],
        TagRpcService.bindService[IO],
        TodoListRpcService.bindService[IO],
        TodoItemRpcService.bindService[IO]
      ).sequence.map(_.map(AddService))

    val runServer = for {
      config <- grpcConfigs
      server <- BuildServerFromConfig[IO]("rpc.server.port", config)
      _      <- GrpcServer.server[IO](server)
    } yield ()

    runServer.unsafeRunSync
  }
}
