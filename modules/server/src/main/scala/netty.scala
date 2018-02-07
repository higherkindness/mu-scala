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

import io.grpc.Server
import io.grpc.netty.{NettyServerBuilder, ProtocolNegotiator}
import io.netty.channel.{ChannelOption, EventLoopGroup, ServerChannel}
import io.netty.handler.ssl.SslContext

object netty {

  sealed trait NettyServerConfig                                           extends Product with Serializable
  case class ChannelType(channelType: Class[_ <: ServerChannel])           extends NettyServerConfig
  case class WithChildOption[T](option: ChannelOption[T], value: T)        extends NettyServerConfig
  case class BossEventLoopGroup(group: EventLoopGroup)                     extends NettyServerConfig
  case class WorkerEventLoopGroup(group: EventLoopGroup)                   extends NettyServerConfig
  case class SetSslContext(sslContext: SslContext)                         extends NettyServerConfig
  case class SetProtocolNegotiator(protocolNegotiator: ProtocolNegotiator) extends NettyServerConfig
  case class MaxConcurrentCallsPerConnection(maxCalls: Int)                extends NettyServerConfig
  case class FlowControlWindow(flowControlWindow: Int)                     extends NettyServerConfig
  case class MaxMessageSize(maxMessageSize: Int)                           extends NettyServerConfig
  case class MaxHeaderListSize(maxHeaderListSize: Int)                     extends NettyServerConfig
  case class KeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit)        extends NettyServerConfig
  case class KeepAliveTimeout(keepAliveTimeout: Long, timeUnit: TimeUnit)  extends NettyServerConfig
  case class MaxConnectionIdle(maxConnectionIdle: Long, timeUnit: TimeUnit)
      extends NettyServerConfig
  case class MaxConnectionAge(maxConnectionAge: Long, timeUnit: TimeUnit) extends NettyServerConfig
  case class MaxConnectionAgeGrace(maxConnectionAgeGrace: Long, timeUnit: TimeUnit)
      extends NettyServerConfig
  case class PermitKeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit) extends NettyServerConfig
  case class PermitKeepAliveWithoutCalls(permit: Boolean)                 extends NettyServerConfig

  final case class NettyServerConfigBuilder(
      initConfig: ChannelFor,
      configList: List[NettyServerConfig]) {

    def build: Server = {
      val builder = initConfig match {
        case ChannelForPort(port)        => NettyServerBuilder.forPort(port)
        case ChannelForSocketAddress(sa) => NettyServerBuilder.forAddress(sa)
        case e =>
          throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
      }

      configList
        .foldLeft(builder) { (acc, cfg) =>
          cfg match {
            case ChannelType(channelType)             => acc.channelType(channelType)
            case WithChildOption(option, value)       => acc.withChildOption(option, value)
            case BossEventLoopGroup(group)            => acc.bossEventLoopGroup(group)
            case WorkerEventLoopGroup(group)          => acc.workerEventLoopGroup(group)
            case SetSslContext(sslContext)            => acc.sslContext(sslContext)
            case SetProtocolNegotiator(pn)            => acc.protocolNegotiator(pn)
            case MaxConcurrentCallsPerConnection(mc)  => acc.maxConcurrentCallsPerConnection(mc)
            case FlowControlWindow(flowControlWindow) => acc.flowControlWindow(flowControlWindow)
            case MaxMessageSize(maxMessageSize)       => acc.maxMessageSize(maxMessageSize)
            case MaxHeaderListSize(maxHeaderListSize) => acc.maxHeaderListSize(maxHeaderListSize)
            case KeepAliveTime(kat, timeUnit)         => acc.keepAliveTime(kat, timeUnit)
            case KeepAliveTimeout(kato, timeUnit)     => acc.keepAliveTimeout(kato, timeUnit)
            case MaxConnectionIdle(mci, tu)           => acc.maxConnectionIdle(mci, tu)
            case MaxConnectionAge(mca, tu)            => acc.maxConnectionAge(mca, tu)
            case MaxConnectionAgeGrace(mcag, tu)      => acc.maxConnectionAgeGrace(mcag, tu)
            case PermitKeepAliveTime(kat, tu)         => acc.permitKeepAliveTime(kat, tu)
            case PermitKeepAliveWithoutCalls(permit)  => acc.permitKeepAliveWithoutCalls(permit)
          }
        }
        .build()
    }
  }
}
