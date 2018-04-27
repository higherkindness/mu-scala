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
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import examples.todolist.persistence.runtime._
import examples.todolist.persistence._
import examples.todolist.protocol.Protocols._
import examples.todolist.runtime.CommonRuntime
import examples.todolist.server.handlers._
import freestyle.rpc.server._
import freestyle.rpc.server.config.BuildServerFromConfig
import freestyle.rpc.server.{AddService, GrpcConfig, ServerW}
import freestyle.tagless.config.implicits._
import freestyle.tagless.loggingJVM.log4s.implicits._
import java.util.Properties

trait ServerImplicits extends CommonRuntime with RepositoriesImplicits {

  implicit val pingPongServiceHandler: PingPongServiceHandler[IO] =
    new PingPongServiceHandler[IO]()

  implicit val tagRpcServiceHandler: TagRpcServiceHandler[IO] =
    new TagRpcServiceHandler[IO]()

  implicit val todoListRpcServiceHandler: TodoListRpcServiceHandler[IO] =
    new TodoListRpcServiceHandler[IO]()

  implicit val todoItemmRpcServiceHandler: TodoItemRpcServiceHandler[IO] =
    new TodoItemRpcServiceHandler[IO] {}

  val grpcConfigs: List[GrpcConfig] =
    List(
      AddService(PingPongService.bindService[IO]),
      AddService(TagRpcService.bindService[IO]),
      AddService(TodoListRpcService.bindService[IO]),
      AddService(TodoItemRpcService.bindService[IO])
    )

  implicit val serverW: ServerW =
    BuildServerFromConfig[IO]("rpc.server.port", grpcConfigs).unsafeRunSync()
}

trait RepositoriesImplicits {

  implicit val xa: HikariTransactor[IO] =
    HikariTransactor[IO](new HikariDataSource(new HikariConfig(new Properties {
      setProperty("driverClassName", "org.h2.Driver")
      setProperty("jdbcUrl", "jdbc:h2:mem:todo")
      setProperty("username", "sa")
      setProperty("password", "")
      setProperty("maximumPoolSize", "10")
      setProperty("minimumIdle", "10")
      setProperty("idleTimeout", "600000")
      setProperty("connectionTimeout", "30000")
      setProperty("connectionTestQuery", "SELECT 1")
      setProperty("maxLifetime", "1800000")
      setProperty("autoCommit", "true")
    })))

  implicit def tagRepositoryHandler(implicit T: Transactor[IO]): TagRepository.Handler[IO] =
    new TagRepositoryHandler[IO]

  implicit def todoListRepositoryHandler(
      implicit T: Transactor[IO]): TodoListRepository.Handler[IO] =
    new TodoListRepositoryHandler[IO]

  implicit def todoItemRespositoryHandler(
      implicit T: Transactor[IO]): TodoItemRepository.Handler[IO] =
    new TodoItemRepositoryHandler[IO]
}

object implicits extends ServerImplicits
