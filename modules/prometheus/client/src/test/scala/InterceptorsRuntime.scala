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

package freestyle.rpc
package prometheus
package client

import freestyle.rpc.client._
import freestyle.rpc.common.ConcurrentMonad
import freestyle.rpc.prometheus.client.implicits.{createManagedChannelForPort, serverW}
import freestyle.rpc.prometheus.shared.Configuration
import freestyle.rpc.withouttagless.Utils._
import freestyle.rpc.withouttagless.Utils.handlers.client.FreesRPCServiceClientHandler
import io.prometheus.client.CollectorRegistry

trait InterceptorsRuntime extends CommonUtils {

  import service._
  import handlers.server._
  import freestyle.rpc.server._
  import freestyle.rpc.server.implicits._

  //////////////////////////////////
  // Server Runtime Configuration //
  //////////////////////////////////

  lazy val grpcConfigs: List[GrpcConfig] = List(
    AddService(RPCService.bindService[ConcurrentMonad])
  )

  implicit lazy val serverW: ServerW = createServerConfOnRandomPort(grpcConfigs)

  implicit lazy val freesRPCHandler: ServerRPCService[ConcurrentMonad] =
    new ServerRPCService[ConcurrentMonad]

}

object implicits extends InterceptorsRuntime

case class ClientRuntime(
    configuration: Configuration,
    cr: CollectorRegistry = new CollectorRegistry()) {

  import service._
  import handlers.client._
  import freestyle.rpc.client.implicits._
  import freestyle.async.catsEffect.implicits._

  implicit val CR: CollectorRegistry = cr

  implicit lazy val freesRPCServiceClient: RPCService.Client[ConcurrentMonad] =
    RPCService.client[ConcurrentMonad](
      channelFor = createManagedChannelForPort(serverW.port),
      channelConfigList = List(
        UsePlaintext(true),
        AddInterceptor(MonitoringClientInterceptor(configuration.withCollectorRegistry(cr)))
      )
    )

  implicit lazy val freesRPCServiceClientHandler: FreesRPCServiceClientHandler[ConcurrentMonad] =
    new FreesRPCServiceClientHandler[ConcurrentMonad]

}
