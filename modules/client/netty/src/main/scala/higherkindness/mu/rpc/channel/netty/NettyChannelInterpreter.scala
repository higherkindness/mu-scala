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

import higherkindness.mu.rpc._
import higherkindness.mu.rpc.channel._
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder

class NettyChannelInterpreter(
    initConfig: ChannelFor,
    configList: List[ManagedChannelConfig],
    nettyConfigList: List[NettyChannelConfig]
) {

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
