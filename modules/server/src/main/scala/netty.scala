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
package server

import java.util.concurrent.TimeUnit

import io.grpc._
import io.grpc.netty.{NettyServerBuilder, ProtocolNegotiator}
import io.netty.channel.{ChannelOption, EventLoopGroup, ServerChannel}
import io.netty.handler.ssl.SslContext

object netty {

  case class ChannelType(channelType: Class[_ <: ServerChannel])            extends GrpcConfig
  case class WithChildOption[T](option: ChannelOption[T], value: T)         extends GrpcConfig
  case class BossEventLoopGroup(group: EventLoopGroup)                      extends GrpcConfig
  case class WorkerEventLoopGroup(group: EventLoopGroup)                    extends GrpcConfig
  case class SetSslContext(sslContext: SslContext)                          extends GrpcConfig
  case class SetProtocolNegotiator(protocolNegotiator: ProtocolNegotiator)  extends GrpcConfig
  case class MaxConcurrentCallsPerConnection(maxCalls: Int)                 extends GrpcConfig
  case class FlowControlWindow(flowControlWindow: Int)                      extends GrpcConfig
  case class MaxMessageSize(maxMessageSize: Int)                            extends GrpcConfig
  case class MaxHeaderListSize(maxHeaderListSize: Int)                      extends GrpcConfig
  case class KeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit)         extends GrpcConfig
  case class KeepAliveTimeout(keepAliveTimeout: Long, timeUnit: TimeUnit)   extends GrpcConfig
  case class MaxConnectionIdle(maxConnectionIdle: Long, timeUnit: TimeUnit) extends GrpcConfig
  case class MaxConnectionAge(maxConnectionAge: Long, timeUnit: TimeUnit)   extends GrpcConfig
  case class MaxConnectionAgeGrace(maxConnectionAgeGrace: Long, timeUnit: TimeUnit)
      extends GrpcConfig
  case class PermitKeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit) extends GrpcConfig
  case class PermitKeepAliveWithoutCalls(permit: Boolean)                 extends GrpcConfig

  final case class NettyServerConfigBuilder(initConfig: ChannelFor, configList: List[GrpcConfig]) {

    def build: Server =
      buildNettyConfig(
        initConfig match {
          case ChannelForPort(port)        => NettyServerBuilder.forPort(port)
          case ChannelForSocketAddress(sa) => NettyServerBuilder.forAddress(sa)
          case e =>
            throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
        },
        configList
      )
  }
}
