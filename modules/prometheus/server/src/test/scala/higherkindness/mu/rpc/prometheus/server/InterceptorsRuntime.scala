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

package higherkindness.mu.rpc
package prometheus
package server

import cats.effect.Resource
import higherkindness.mu.rpc.common.ConcurrentMonad
import higherkindness.mu.rpc.prometheus.shared.Configuration
import higherkindness.mu.rpc.protocol.Utils._
import io.prometheus.client.CollectorRegistry

case class InterceptorsRuntime(
    configuration: Configuration,
    cr: CollectorRegistry = new CollectorRegistry())
    extends CommonUtils {

  import TestsImplicits._
  import service._
  import handlers.server._
  import handlers.client._
  import higherkindness.mu.rpc.server._
  import higherkindness.mu.rpc.interceptors.implicits._
  import cats.instances.list._
  import cats.syntax.traverse._

  //////////////////////////////////
  // Server Runtime Configuration //
  //////////////////////////////////

  lazy val monitorInterceptor = MonitoringServerInterceptor(configuration.withCollectorRegistry(cr))

  implicit val CR: CollectorRegistry = cr

  lazy val grpcConfigs: ConcurrentMonad[List[GrpcConfig]] = List(
    ProtoRPCService.bindService[ConcurrentMonad],
    AvroRPCService.bindService[ConcurrentMonad],
    AvroWithSchemaRPCService.bindService[ConcurrentMonad]
  ).sequence.map(_.map(_.interceptWith(monitorInterceptor)).map(AddService))

  implicit lazy val grpcServer: GrpcServer[ConcurrentMonad] =
    grpcConfigs.flatMap(createServerConfOnRandomPort[ConcurrentMonad]).unsafeRunSync

  implicit lazy val muRPCHandler: ServerRPCService[ConcurrentMonad] =
    new ServerRPCService[ConcurrentMonad]

  //////////////////////////////////
  // Client Runtime Configuration //
  //////////////////////////////////

  lazy val muProtoRPCServiceClient: Resource[ConcurrentMonad, ProtoRPCService[ConcurrentMonad]] =
    ProtoRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort)
    )

  lazy val muAvroRPCServiceClient: Resource[ConcurrentMonad, AvroRPCService[ConcurrentMonad]] =
    AvroRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort)
    )

  lazy val muAvroWithSchemaRPCServiceClient: Resource[
    ConcurrentMonad,
    AvroWithSchemaRPCService[ConcurrentMonad]] =
    AvroWithSchemaRPCService.client[ConcurrentMonad](
      channelFor = createChannelForPort(pickUnusedPort)
    )

  implicit lazy val muRPCServiceClientHandler: MuRPCServiceClientHandler[ConcurrentMonad] =
    new MuRPCServiceClientHandler[ConcurrentMonad](
      muProtoRPCServiceClient,
      muAvroRPCServiceClient,
      muAvroWithSchemaRPCServiceClient)

}
