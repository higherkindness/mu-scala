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
package client

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder

package object netty {

  class NettyChannelInterpreter(
      initConfig: ChannelFor,
      configList: List[ManagedChannelConfig],
      nettyConfigList: List[NettyChannelConfig]) {

    def build: ManagedChannel = {
      val builder: NettyChannelBuilder = initConfig match {
        case ChannelForAddress(name, port) => NettyChannelBuilder.forAddress(name, port)
        case ChannelForSocketAddress(sa)   => NettyChannelBuilder.forAddress(sa)
        case ChannelForTarget(target)      => NettyChannelBuilder.forTarget(target)
        case e =>
          throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
      }

      val baseBuilder: NettyChannelBuilder => NettyChannelBuilder =
        configList.foldLeft(_)((acc, cfg) => configureChannel(acc, cfg))
      val nettyBuilder: NettyChannelBuilder => NettyChannelBuilder =
        nettyConfigList.foldLeft(_)((acc, cfg) => configureNettyChannel(acc)(cfg))

      (baseBuilder andThen nettyBuilder)(builder).build()
    }
  }

  def configureNettyChannel(mcb: NettyChannelBuilder): NettyChannelConfig => NettyChannelBuilder = {
    case NettyChannelType(channelType)       => mcb.channelType(channelType)
    case NettyWithOption(option, value)      => mcb.withOption(option, value)
    case NettyNegotiationType(nt)            => mcb.negotiationType(nt)
    case NettyEventLoopGroup(eventLoopGroup) => mcb.eventLoopGroup(eventLoopGroup)
    case NettySslContext(sslContext)         => mcb.sslContext(sslContext)
    case NettyFlowControlWindow(fcw)         => mcb.flowControlWindow(fcw)
    case NettyMaxHeaderListSize(mhls)        => mcb.maxInboundMetadataSize(mhls)
    case NettyUsePlaintext()                 => mcb.usePlaintext()
    case NettyUseTransportSecurity           => mcb.useTransportSecurity()
    case NettyKeepAliveTime(kat, tu)         => mcb.keepAliveTime(kat, tu)
    case NettyKeepAliveTimeout(kat, tu)      => mcb.keepAliveTimeout(kat, tu)
    case NettyKeepAliveWithoutCalls(enable)  => mcb.keepAliveWithoutCalls(enable)
  }
}
