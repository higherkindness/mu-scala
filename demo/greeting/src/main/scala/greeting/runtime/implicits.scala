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
package greeting.runtime

import cats.~>
import freestyle._
import freestyle.implicits._
import freestyle.async.implicits._
import freestyle.rpc.demo.echo.EchoServiceGrpc
import freestyle.rpc.demo.greeting._
import freestyle.rpc.demo.greeting.service._

import scala.concurrent.{ExecutionContext, Future}

trait ServerImplicits {

  import freestyle.rpc.server._
  import freestyle.rpc.server.handlers._
  import freestyle.rpc.server.implicits._

  implicit val config: Config       = Config(portNode1)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit val grpcConfigs: List[GrpcConfig] = List(
    AddService(GreeterGrpc.bindService(new GreetingService, ec)),
    AddService(EchoServiceGrpc.bindService(new EchoService, ec))
  )

  implicit val grpcServerHandler =
    new GrpcServerHandler[Future] andThen new GrpcConfigInterpreter[Future]
}

trait ClientImplicits {

  import freestyle.rpc.client._
  import freestyle.rpc.client.implicits._
  import freestyle.rpc.client.handlers.{ChannelMHandler, ClientCallsMHandler}

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val channelFor: ManagedChannelFor = ManagedChannelForAddress(host, portNode1)

  val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext(true))

  implicit def channelMHandler[F[_]]: ChannelM.Op ~> Future =
    new ChannelMHandler[Future] andThen
      new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

  implicit val clientCallsMHandler: ClientCallsM.Op ~> Future = new ClientCallsMHandler[Future]

}

object implicits {

  object server extends ServerImplicits

  object client extends ClientImplicits
}
