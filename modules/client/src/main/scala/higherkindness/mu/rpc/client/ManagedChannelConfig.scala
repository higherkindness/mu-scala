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

import java.util.concurrent.{Executor, TimeUnit}

import io.grpc._

sealed trait ManagedChannelConfig                extends Product with Serializable
case object DirectExecutor                       extends ManagedChannelConfig
final case class SetExecutor(executor: Executor) extends ManagedChannelConfig
final case class AddInterceptorList(interceptors: List[ClientInterceptor])
    extends ManagedChannelConfig
final case class AddInterceptor(interceptors: ClientInterceptor*) extends ManagedChannelConfig
final case class UserAgent(userAgent: String)                     extends ManagedChannelConfig
final case class OverrideAuthority(authority: String)             extends ManagedChannelConfig
final case class UsePlaintext()                                   extends ManagedChannelConfig
final case class NameResolverFactory(resolverFactory: NameResolver.Factory)
    extends ManagedChannelConfig
final case class LoadBalancerFactory(lbFactory: LoadBalancer.Factory) extends ManagedChannelConfig
final case class SetDecompressorRegistry(registry: DecompressorRegistry)
    extends ManagedChannelConfig
final case class SetCompressorRegistry(registry: CompressorRegistry) extends ManagedChannelConfig
final case class SetIdleTimeout(value: Long, unit: TimeUnit)         extends ManagedChannelConfig
final case class SetMaxInboundMessageSize(max: Int)                  extends ManagedChannelConfig
