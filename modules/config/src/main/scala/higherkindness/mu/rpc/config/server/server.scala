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

package higherkindness.mu.rpc
package config

import higherkindness.mu.rpc.server.{GrpcConfig, GrpcServer}

package object server {

  def BuildServerFromConfig[F[_]](portPath: String, configList: List[GrpcConfig] = Nil)(
      implicit SC: ServerConfig[F]
  ): F[GrpcServer[F]] =
    SC.buildServer(portPath, configList)

  def BuildNettyServerFromConfig[F[_]](
      portPath: String,
      configList: List[GrpcConfig] = Nil
  )(implicit SC: ServerConfig[F]): F[GrpcServer[F]] =
    SC.buildNettyServer(portPath, configList)

}
