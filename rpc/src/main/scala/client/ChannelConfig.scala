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

import cats.syntax.either._
import java.util.concurrent.{Executor, TimeUnit}

import freestyle.free._
import freestyle.free.config.ConfigM
import io.grpc._

sealed trait ManagedChannelFor                               extends Product with Serializable
case class ManagedChannelForAddress(host: String, port: Int) extends ManagedChannelFor
case class ManagedChannelForTarget(target: String)           extends ManagedChannelFor

sealed trait ManagedChannelConfig                                     extends Product with Serializable
case object DirectExecutor                                            extends ManagedChannelConfig
case class SetExecutor(executor: Executor)                            extends ManagedChannelConfig
case class AddInterceptorList(interceptors: List[ClientInterceptor])  extends ManagedChannelConfig
case class AddInterceptor(interceptors: ClientInterceptor*)           extends ManagedChannelConfig
case class UserAgent(userAgent: String)                               extends ManagedChannelConfig
case class OverrideAuthority(authority: String)                       extends ManagedChannelConfig
case class UsePlaintext(skipNegotiation: Boolean)                     extends ManagedChannelConfig
case class NameResolverFactory(resolverFactory: NameResolver.Factory) extends ManagedChannelConfig
case class LoadBalancerFactory(lbFactory: LoadBalancer.Factory)       extends ManagedChannelConfig
case class SetDecompressorRegistry(registry: DecompressorRegistry)    extends ManagedChannelConfig
case class SetCompressorRegistry(registry: CompressorRegistry)        extends ManagedChannelConfig
case class SetIdleTimeout(value: Long, unit: TimeUnit)                extends ManagedChannelConfig
case class SetMaxInboundMessageSize(max: Int)                         extends ManagedChannelConfig

@module
trait ChannelConfig {

  val configM: ConfigM
  val defaultHost = "localhost"
  val defaultPort = 50051

  def loadChannelAddress(hostPath: String, portPath: String): FS.Seq[ManagedChannelForAddress] =
    configM.load map (config =>
      ManagedChannelForAddress(
        config.string(hostPath).getOrElse(defaultHost),
        config.int(portPath).getOrElse(defaultPort)))

  def loadChannelTarget(targetPath: String): FS.Seq[ManagedChannelForTarget] =
    configM.load map (config =>
      ManagedChannelForTarget(config.string(targetPath).getOrElse("target")))
}
