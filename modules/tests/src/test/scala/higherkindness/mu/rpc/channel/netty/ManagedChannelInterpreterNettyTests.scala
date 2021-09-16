/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.channel.netty

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.channel._
import higherkindness.mu.rpc.common.SC
import cats.effect.IO
import io.grpc.ManagedChannel
import io.grpc.internal.GrpcUtil
import io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.netty.channel.ChannelOption
import io.netty.channel.local.LocalChannel
import io.netty.channel.nio.NioEventLoopGroup

class ManagedChannelInterpreterNettyTests extends ManagedChannelInterpreterTests {

  def mkInterpreter(
      channelFor: ChannelFor,
      channelConfigList: List[ManagedChannelConfig]
  ): ManagedChannelInterpreter[IO] =
    new ManagedChannelInterpreter[IO](
      channelFor,
      channelConfigList,
      NettyChannelBuilder.forAddress,
      NettyChannelBuilder.forTarget
    )

  test(
    "NettyChannelInterpreter build an io.grpc.ManagedChannel based on the specified configuration, for a Socket Address"
  ) {

    val channelFor: ChannelFor = ChannelForSocketAddress(new InetSocketAddress(SC.host, 45455))

    val channelConfigList = List(UsePlaintext())

    val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList, Nil)

    val mc: ManagedChannel = interpreter.build

    assert(Option(mc).nonEmpty)

    mc.shutdownNow()
  }

  test(
    "NettyChannelInterpreter build an io.grpc.ManagedChannel based on the specified configuration, for a Target"
  ) {

    val channelFor: ChannelFor = ChannelForTarget(SC.host)

    val interpreter = new NettyChannelInterpreter(channelFor, Nil, Nil)

    val mc: ManagedChannel = interpreter.build

    assert(Option(mc).nonEmpty)

    mc.shutdownNow()
  }

  test(
    "NettyChannelInterpreter build an io.grpc.ManagedChannel based on any configuration combination"
  ) {

    val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

    val nettyChannelConfigList: List[NettyChannelConfig] = List(
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

    val interpreter =
      new NettyChannelInterpreter(
        channelFor,
        TestData.managedChannelConfigAllList,
        nettyChannelConfigList
      )

    val mc: ManagedChannel = interpreter.build

    assert(Option(mc).nonEmpty)

    mc.shutdownNow()
  }

  test("NettyChannelInterpreter throw an exception when ChannelFor is not recognized") {

    val channelFor: ChannelFor = ChannelForPort(SC.port)

    val interpreter = new NettyChannelInterpreter(channelFor, Nil, Nil)

    intercept[IllegalArgumentException](interpreter.build)
  }

}
