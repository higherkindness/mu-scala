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
package config

import cats.Functor
import cats.syntax.either._
import cats.syntax.functor._
import freestyle.tagless._
import freestyle.tagless.config.ConfigM

@module
trait ChannelConfig {

  implicit val functor: Functor

  val configM: ConfigM
  val defaultHost: String = "localhost"
  val defaultPort: Int    = freestyle.rpc.server.defaultPort

  def loadChannelAddress(hostPath: String, portPath: String): FS[ChannelForAddress] =
    configM.load map (config =>
      ChannelForAddress(
        config.string(hostPath).getOrElse(defaultHost),
        config.int(portPath).getOrElse(defaultPort)))

  def loadChannelTarget(targetPath: String): FS[ChannelForTarget] =
    configM.load map (config => ChannelForTarget(config.string(targetPath).getOrElse("target")))
}
