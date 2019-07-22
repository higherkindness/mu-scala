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

package higherkindness.mu.rpc.healthcheck.client

import cats.effect.IO
import higherkindness.mu.rpc.healthcheck.ServerStatus
import monix.execution.Scheduler

object ClientProgram {

  def clientProgramIO(who: String, mode: String)(
      implicit handler: HealthCheckClientHandler[IO],
      sch: Scheduler) =
    mode match {
      case "watch" =>
        for {
          _ <- IO.delay(println("///////////////////////Starting watching"))
          _ <- handler.watching("example" + who)
          _ <- IO.delay(println("///////////////////////Exiting watching"))
        } yield ()

      case "simple" =>
        for {
          _ <- IO.delay(println("///////////////////////Starting program"))
          _ <- handler.settingAndCheck("example", ServerStatus("SERVING"))
          _ <- handler.settingAndFullClean(
            List(("example1", ServerStatus("SERVING")), ("example2", ServerStatus("SERVING"))))
          _ <- IO.delay(println("///////////////////////Exiting program"))
        } yield ()

      case "update" =>
        for {
          _ <- IO.delay(println("///////////////////////Starting program"))
          _ <- handler.updatingWatching("example" + who)
          _ <- handler.updatingWatching("example3")
          _ <- IO.delay(println("///////////////////////Exiting program"))
        } yield ()

    }

}
