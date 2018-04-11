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
package netty

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import freestyle.rpc.common.SC
import io.grpc.ManagedChannel
import io.grpc.internal.GrpcUtil
import io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.netty.channel.ChannelOption
import io.netty.channel.local.LocalChannel
import io.netty.channel.nio.NioEventLoopGroup

class ManagedChannelInterpreterNettyTests extends ManagedChannelInterpreterTests {

  import implicits._

  "NettyChannelInterpreter" should {

    "build an io.grpc.ManagedChannel based on the specified configuration, for a Socket Address" in {

      val channelFor: ChannelFor = ChannelForSocketAddress(new InetSocketAddress(SC.host, 45455))

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

      val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "build an io.grpc.ManagedChannel based on the specified configuration, for a Target" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val interpreter = new NettyChannelInterpreter(channelFor, Nil)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "build an io.grpc.ManagedChannel based on any configuration combination" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[ManagedChannelConfig] = managedChannelConfigAllList ++ List(
        NettyChannelType((new LocalChannel).getClass),
        NettyWithOption[Boolean](ChannelOption.valueOf("ALLOCATOR"), true),
        NettyNegotiationType(NegotiationType.PLAINTEXT),
        NettyEventLoopGroup(new NioEventLoopGroup(0)),
        NettySslContext(GrpcSslContexts.forClient.build),
        NettyFlowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW),
        NettyMaxHeaderListSize(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE),
        NettyUsePlaintext(),
        NettyUseTransportSecurity,
        NettyKeepAliveTime(1, TimeUnit.MINUTES),
        NettyKeepAliveTimeout(1, TimeUnit.MINUTES),
        NettyKeepAliveWithoutCalls(false)
      )

      val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "throw an exception when configuration is not recognized" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      case object Unexpected extends ManagedChannelConfig
      val channelConfigList: List[ManagedChannelConfig] = List(Unexpected)

      val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList)

      a[MatchError] shouldBe thrownBy(interpreter.build)
    }

    "throw an exception when ChannelFor is not recognized" in {

      val channelFor: ChannelFor = ChannelForPort(SC.port)

      val interpreter = new NettyChannelInterpreter(channelFor, Nil)

      an[IllegalArgumentException] shouldBe thrownBy(interpreter.build)
    }
  }

}
