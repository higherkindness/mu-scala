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

import cats.effect.{Resource, Sync}
import higherkindness.mu.rpc.healthcheck.ServerStatus
import higherkindness.mu.rpc.healthcheck.handler.service.{
  HealthCheck,
  HealthCheckService,
  HealthStatus
}
import cats.syntax._
import cats.implicits._
import org.log4s.{getLogger, Logger}

class HealthCheckClientHandler[F[_]: Sync](client: Resource[F, HealthCheckService[F]]) {
  val logger: Logger = getLogger

  def settingAndCheck(name: String, status: ServerStatus) =
    for {
      _ <- Sync[F].delay(
        println("/////////////////////////Nos metemos en la parte del healthcheck"))
      wentNice <- client.use(_.setStatus(HealthStatus(HealthCheck(name), status)))
      _ <- Sync[F].delay(
        logger.info(
          "Added status: " + status.toString + "to service: " + name + ". It went ok?" + wentNice))
      status <- client.use(_.check(HealthCheck(name)))
      _      <- Sync[F].delay(logger.info("Checked the status of " + name + ". Obtained: " + status))
    } yield ()

}
