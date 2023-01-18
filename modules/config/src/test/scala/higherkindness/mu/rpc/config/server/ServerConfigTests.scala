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

package higherkindness.mu.rpc.config.server

import cats.effect.IO
import higherkindness.mu.rpc.server._
import munit.CatsEffectSuite

import scala.concurrent.ExecutionContext

class ServerConfigTests extends CatsEffectSuite {

  val port: Int    = 51111
  val host: String = "localhost"

  val ec: ExecutionContext = ExecutionContext.Implicits.global

  test("ServerConfig load the port specified in the config file") {

    val loadAndPort = for {
      server <- BuildServerFromConfig[IO]("rpc.server.port")
      _      <- server.start
      port   <- server.getPort
      _      <- server.shutdownNow
      _      <- server.awaitTermination
    } yield port

    loadAndPort.assertEquals(port)
  }

  test("ServerConfig load the default port when the config port path is not found") {

    val loadAndPort = for {
      server <- BuildServerFromConfig[IO]("rpc.wrong.port")
      _      <- server.start
      port   <- server.getPort
      _      <- server.shutdownNow
      _      <- server.awaitTermination
    } yield port

    loadAndPort.assertEquals(defaultPort)
  }
}
