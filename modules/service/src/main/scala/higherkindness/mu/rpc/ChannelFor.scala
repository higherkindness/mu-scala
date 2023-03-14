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

package higherkindness.mu.rpc

import java.net.SocketAddress

sealed trait ChannelFor                                     extends Product with Serializable
final case class ChannelForPort(port: Int)                  extends ChannelFor
final case class ChannelForAddress(host: String, port: Int) extends ChannelFor
final case class ChannelForSocketAddress(serverAddress: SocketAddress) extends ChannelFor
final case class ChannelForTarget(target: String)                      extends ChannelFor
