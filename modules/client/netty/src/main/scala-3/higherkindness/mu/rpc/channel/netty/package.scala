/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.channel

import io.grpc.netty.NettyChannelBuilder
import scala.annotation.nowarn

package object netty {

  @nowarn("msg=Unreachable case") // see https://github.com/lampepfl/dotty/issues/14807
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
