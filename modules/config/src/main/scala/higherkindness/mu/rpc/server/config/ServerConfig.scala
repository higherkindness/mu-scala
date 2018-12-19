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
package server.config

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.either._
import higherkindness.mu.rpc.config.ConfigM
import com.typesafe.config.ConfigException.Missing
import higherkindness.mu.rpc.server._

class ServerConfig[F[_]](implicit S: Sync[F], C: ConfigM[F]) {

  def buildServer(portPath: String, configList: List[GrpcConfig] = Nil): F[GrpcServer[F]] =
    for {
      config <- C.load
      port = Either.catchOnly[Missing](config.getInt(portPath))
      server <- GrpcServer.default(port.getOrElse(defaultPort), configList)
    } yield server

  def buildNettyServer(portPath: String, configList: List[GrpcConfig] = Nil): F[GrpcServer[F]] =
    for {
      config <- C.load
      port = Either.catchOnly[Missing](config.getInt(portPath))
      server <- GrpcServer.netty(ChannelForPort(port.getOrElse(defaultPort)), configList)
    } yield server
}

object ServerConfig {
  def apply[F[_]](implicit SC: ServerConfig[F]): ServerConfig[F] = SC

  implicit def defaultServerConfig[F[_]](implicit S: Sync[F], C: ConfigM[F]): ServerConfig[F] =
    new ServerConfig[F]()(S, C)
}
