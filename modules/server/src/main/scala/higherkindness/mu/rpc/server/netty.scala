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

package higherkindness.mu.rpc
package server

import java.util.concurrent.TimeUnit

import io.grpc.netty.ProtocolNegotiator
import io.netty.channel.{ChannelOption, EventLoopGroup, ServerChannel}
import io.netty.handler.ssl.SslContext

object netty {

  final case class ChannelType(channelType: Class[_ <: ServerChannel])            extends GrpcConfig
  final case class WithChildOption[T](option: ChannelOption[T], value: T)         extends GrpcConfig
  final case class BossEventLoopGroup(group: EventLoopGroup)                      extends GrpcConfig
  final case class WorkerEventLoopGroup(group: EventLoopGroup)                    extends GrpcConfig
  final case class SetSslContext(sslContext: SslContext)                          extends GrpcConfig
  final case class SetProtocolNegotiator(protocolNegotiator: ProtocolNegotiator)  extends GrpcConfig
  final case class MaxConcurrentCallsPerConnection(maxCalls: Int)                 extends GrpcConfig
  final case class FlowControlWindow(flowControlWindow: Int)                      extends GrpcConfig
  final case class MaxMessageSize(maxMessageSize: Int)                            extends GrpcConfig
  final case class MaxHeaderListSize(maxHeaderListSize: Int)                      extends GrpcConfig
  final case class KeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit)         extends GrpcConfig
  final case class KeepAliveTimeout(keepAliveTimeout: Long, timeUnit: TimeUnit)   extends GrpcConfig
  final case class MaxConnectionIdle(maxConnectionIdle: Long, timeUnit: TimeUnit) extends GrpcConfig
  final case class MaxConnectionAge(maxConnectionAge: Long, timeUnit: TimeUnit)   extends GrpcConfig
  final case class MaxConnectionAgeGrace(maxConnectionAgeGrace: Long, timeUnit: TimeUnit)
      extends GrpcConfig
  final case class PermitKeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit) extends GrpcConfig
  final case class PermitKeepAliveWithoutCalls(permit: Boolean)                 extends GrpcConfig

}
