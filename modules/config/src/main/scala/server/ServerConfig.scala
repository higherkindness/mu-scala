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
package server
package config

import cats.Functor
import cats.syntax.either._
import cats.syntax.functor._
import freestyle.tagless._
import freestyle.tagless.config.ConfigM

@module
trait ServerConfig {

  val configM: ConfigM
  implicit val functor: Functor

  def buildServer(portPath: String, configList: List[GrpcConfig] = Nil): FS[ServerW] =
    configM.load.map { config =>
      val port = config.int(portPath).getOrElse(defaultPort)
      ServerW(port, configList)
    }
}
