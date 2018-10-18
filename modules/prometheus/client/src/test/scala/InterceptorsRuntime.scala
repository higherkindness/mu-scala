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

package mu.rpc
package prometheus
package client

import mu.rpc.client._
import mu.rpc.common.ConcurrentMonad
import mu.rpc.prometheus.shared.Configuration
import mu.rpc.protocol.Utils._
import mu.rpc.protocol.Utils.handlers.client.MuRPCServiceClientHandler
import io.prometheus.client.CollectorRegistry

case class InterceptorsRuntime(
    configuration: Configuration,
    cr: CollectorRegistry = new CollectorRegistry())
    extends CommonUtils {

  import service._
  import handlers.server._
  import handlers.client._
  import mu.rpc.server._

  //////////////////////////////////
  // Server Runtime Configuration //
  //////////////////////////////////

  lazy val grpcConfigs: List[GrpcConfig] = List(
    AddService(ProtoRPCService.bindService[ConcurrentMonad]),
    AddService(AvroRPCService.bindService[ConcurrentMonad]),
    AddService(AvroWithSchemaRPCService.bindService[ConcurrentMonad])
  )

  implicit lazy val grpcServer: GrpcServer[ConcurrentMonad] =
    createServerConfOnRandomPort[ConcurrentMonad](grpcConfigs).unsafeRunSync

  implicit lazy val muRPCHandler: ServerRPCService[ConcurrentMonad] =
    new ServerRPCService[ConcurrentMonad]

  implicit val CR: CollectorRegistry = cr
  val configList = List(
    UsePlaintext(),
    AddInterceptor(MonitoringClientInterceptor(configuration.withCollectorRegistry(cr)))
  )

  implicit lazy val muProtoRPCServiceClient: ProtoRPCService.Client[ConcurrentMonad] =
    ProtoRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort),
      channelConfigList = configList
    )

  implicit lazy val muAvroRPCServiceClient: AvroRPCService.Client[ConcurrentMonad] =
    AvroRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort),
      channelConfigList = configList
    )

  implicit lazy val muAvroWithSchemaRPCServiceClient: AvroWithSchemaRPCService.Client[
    ConcurrentMonad] =
    AvroWithSchemaRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort),
      channelConfigList = configList
    )

  implicit lazy val muRPCServiceClientHandler: MuRPCServiceClientHandler[ConcurrentMonad] =
    new MuRPCServiceClientHandler[ConcurrentMonad]

}
