/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.healthcheck.client.fs2

import cats.effect.IO
import higherkindness.mu.rpc.healthcheck.unary.handler.ServerStatus
import io.chrisdavenport.log4cats.Logger

object ClientProgramFS2 {

  def clientProgramIO(who: String, mode: String)(
      implicit handler: HealthCheckClientHandlerFS2[IO],
      logger: Logger[IO]) = {

    mode match {
      case "watch" =>
        for {
          _ <- logger.info("///////////////////////Starting watching")
          _ <- handler.watching("example" + who)
          _ <- logger.info("///////////////////////Exiting watching")
        } yield ()

      case "simple" =>
        for {
          _ <- logger.info("///////////////////////Starting program")
          _ <- handler.settingAndCheck("example", ServerStatus("SERVING"))
          _ <- handler.settingAndFullClean(
            List(("example1", ServerStatus("SERVING")), ("example2", ServerStatus("SERVING"))))
          _ <- logger.info("///////////////////////Exiting program")
        } yield ()

      case "update" =>
        for {
          _ <- logger.info("///////////////////////Starting program")
          _ <- handler.updatingWatching("example" + who)
          _ <- handler.updatingWatching("example3")
          _ <- logger.info("///////////////////////Exiting program")
        } yield ()

      case _ => logger.info("Wrong input!")
    }
  }
}
