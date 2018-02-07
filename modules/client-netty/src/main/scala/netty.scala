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
package client

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder

package object netty {

  class NettyChannelInterpreter(initConfig: ChannelFor, configList: List[NettyChannelConfig]) {

    def build: ManagedChannel = {
      val builder: NettyChannelBuilder = initConfig match {
        case ChannelForAddress(name, port) => NettyChannelBuilder.forAddress(name, port)
        case ChannelForSocketAddress(sa)   => NettyChannelBuilder.forAddress(sa)
        case ChannelForTarget(target)      => NettyChannelBuilder.forTarget(target)
        case e =>
          throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
      }

      configList
        .foldLeft(builder) { (acc, cfg) =>
          cfg match {
            case NettyChannelType(channelType)       => acc.channelType(channelType)
            case NettyWithOption(option, value)      => acc.withOption(option, value)
            case NettyNegotiationType(nt)            => acc.negotiationType(nt)
            case NettyEventLoopGroup(eventLoopGroup) => acc.eventLoopGroup(eventLoopGroup)
            case NettySslContext(sslContext)         => acc.sslContext(sslContext)
            case NettyFlowControlWindow(fcw)         => acc.flowControlWindow(fcw)
            case NettyMaxHeaderListSize(mhls)        => acc.maxHeaderListSize(mhls)
            case NettyUsePlaintext(skipNegotiation)  => acc.usePlaintext(skipNegotiation)
            case NettyUseTransportSecurity           => acc.useTransportSecurity()
            case NettyKeepAliveTime(kat, tu)         => acc.keepAliveTime(kat, tu)
            case NettyKeepAliveTimeout(kat, tu)      => acc.keepAliveTimeout(kat, tu)
            case NettyKeepAliveWithoutCalls(enable)  => acc.keepAliveWithoutCalls(enable)
          }
        }
        .build()
    }
  }
}
