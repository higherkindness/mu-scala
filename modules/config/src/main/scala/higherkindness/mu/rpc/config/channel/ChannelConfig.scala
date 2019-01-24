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
package config.channel

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.config.ConfigException.Missing
import higherkindness.mu.rpc.config.ConfigM
import higherkindness.mu.rpc.{ChannelForAddress, ChannelForTarget}

class ChannelConfig[F[_]](implicit S: Sync[F], C: ConfigM[F]) {

  val defaultHost: String = "localhost"
  val defaultPort: Int    = higherkindness.mu.rpc.server.defaultPort

  def loadChannelAddress(hostPath: String, portPath: String): F[ChannelForAddress] =
    for {
      config <- C.load
      host <- S.pure(
        Either
          .catchOnly[Missing](config.getString(hostPath)))
      port <- S.pure(Either.catchOnly[Missing](config.getInt(portPath)))
    } yield ChannelForAddress(host.getOrElse(defaultHost), port.getOrElse(defaultPort))

  def loadChannelTarget(targetPath: String): F[ChannelForTarget] =
    for {
      config <- C.load
      target <- S.pure(Either.catchOnly[Missing](config.getString(targetPath)))
    } yield ChannelForTarget(target.getOrElse("target"))

}

object ChannelConfig {
  def apply[F[_]](implicit S: Sync[F], C: ConfigM[F]): ChannelConfig[F] =
    new ChannelConfig[F]

  implicit def defaultChannelConfig[F[_]](implicit S: Sync[F], C: ConfigM[F]): ChannelConfig[F] =
    apply[F](S, C)
}
