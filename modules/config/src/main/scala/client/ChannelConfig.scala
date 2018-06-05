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

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.either._

import freestyle.rpc.config.ConfigM

import com.typesafe.config.ConfigException.Missing

trait ChannelConfig[F[_]] {

  implicit def sync: Sync[F]
  implicit def configM: ConfigM[F]

  val defaultHost: String = "localhost"
  val defaultPort: Int    = freestyle.rpc.server.defaultPort

  def loadChannelAddress(hostPath: String, portPath: String): F[ChannelForAddress] =
    for {
      config <- configM.load
      host <- sync.pure(
        Either
          .catchOnly[Missing](config.getString(hostPath)))
      port <- sync.pure(Either.catchOnly[Missing](config.getInt(portPath)))
    } yield ChannelForAddress(host.getOrElse(defaultHost), port.getOrElse(defaultPort))

  def loadChannelTarget(targetPath: String): F[ChannelForTarget] =
    for {
      config <- configM.load
      target <- sync.pure(Either.catchOnly[Missing](config.getString(targetPath)))
    } yield ChannelForTarget(target.getOrElse("target"))

}

object ChannelConfig {
  def apply[F[_]](implicit S: Sync[F], C: ConfigM[F]): ChannelConfig[F] = new ChannelConfig[F] {
    def sync    = S
    def configM = C
  }

  implicit def defaultChannelConfig[F[_]](implicit S: Sync[F], C: ConfigM[F]): ChannelConfig[F] =
    apply[F](S, C)
}
