/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc.demo
package user.runtime

import cats.implicits._
import cats.~>
import freestyle._
import freestyle.implicits._
import freestyle.config.implicits._
import freestyle.rpc.demo.user._
import freestyle.rpc.server._
import freestyle.rpc.server.implicits._
import freestyle.rpc.server.handlers._
import io.grpc.Server

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object implicits {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val grpcConfigs: List[GrpcConfig] = List(
    AddService(UserServiceGrpc.bindService(new UserService, ExecutionContext.global))
  )

  val serverFromConfig: Server = Await.result(
    BuildServerFromConfig[ServerConfig.Op]("rpc.server.port", grpcConfigs)
      .interpret[Future],
    1.seconds)

  implicit val grpcServerHandler: GrpcServer.Op ~> Future =
    new GrpcServerHandler[Future] andThen
      new GrpcKInterpreter[Future](serverFromConfig)

}
