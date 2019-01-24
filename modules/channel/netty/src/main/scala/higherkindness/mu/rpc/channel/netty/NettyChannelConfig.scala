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
package channel
package netty

import java.util.concurrent.TimeUnit

import io.grpc.netty.NegotiationType
import io.netty.channel.{Channel, ChannelOption, EventLoopGroup}
import io.netty.handler.ssl.SslContext

sealed trait NettyChannelConfig                                         extends Product with Serializable
final case class NettyChannelType(channelType: Class[_ <: Channel])     extends NettyChannelConfig
final case class NettyWithOption[T](option: ChannelOption[T], value: T) extends NettyChannelConfig
final case class NettyNegotiationType(`type`: NegotiationType)          extends NettyChannelConfig
final case class NettyEventLoopGroup(eventLoopGroup: EventLoopGroup)    extends NettyChannelConfig
final case class NettySslContext(sslContext: SslContext)                extends NettyChannelConfig
final case class NettyFlowControlWindow(flowControlWindow: Int)         extends NettyChannelConfig
final case class NettyMaxHeaderListSize(maxHeaderListSize: Int)         extends NettyChannelConfig
final case class NettyUsePlaintext()                                    extends NettyChannelConfig
case object NettyUseTransportSecurity                                   extends NettyChannelConfig
final case class NettyKeepAliveTime(keepAliveTime: Long, timeUnit: TimeUnit)
    extends NettyChannelConfig
final case class NettyKeepAliveTimeout(keepAliveTimeout: Long, timeUnit: TimeUnit)
    extends NettyChannelConfig
final case class NettyKeepAliveWithoutCalls(enable: Boolean) extends NettyChannelConfig
