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

package freestyle.rpc
package demo
package protocolgen
package runtime

import cats.~>
import freestyle.rpc.demo.protocolgen.protocols.GreetingService

import scala.concurrent.{ExecutionContext, Future}

trait CommonImplicits {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

}

object server {

  trait Implicits extends CommonImplicits {

    import freestyle.rpc.server._
    import freestyle.rpc.server.handlers._
    import freestyle.rpc.server.implicits._

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(GreetingService.bindService)
    )

    val conf: ServerW = ServerW(50051, grpcConfigs)

    implicit val grpcServerHandler: GrpcServer.Op ~> Future =
      new GrpcServerHandler[Future] andThen
        new GrpcKInterpreter[Future](conf.server)

  }

  object implicits extends Implicits

}
