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
  EmptyInput,
  HealthCheck,
  HealthCheckService,
  HealthStatus,
  WentNice
}
import cats.syntax._
import cats.implicits._
import org.log4s.{getLogger, Logger}

class HealthCheckClientHandler[F[_]: Sync](client: Resource[F, HealthCheckService[F]]) {
  val logger: Logger = getLogger

  def settingAndCheck(name: String, status: ServerStatus) =
    for {
      _     <- Sync[F].delay(println("Is there some server named " + name.toUpperCase + "?"))
      known <- client.use(_.check(HealthCheck(name)))
      _     <- Sync[F].delay(println("Actually the status is " + known.status))
      _ <- Sync[F].delay(
        println("Setting " + name.toUpperCase + " service with " + status.status + " status"))
      wentNiceSet <- client.use(_.setStatus(HealthStatus(HealthCheck(name), status)))
      _ <- Sync[F].delay(println(
        "Went it ok? " + wentNiceSet.ok + ". Added status: " + status.status + " to service: " + name.toUpperCase))
      status <- client.use(_.check(HealthCheck(name)))
      _ <- Sync[F].delay(
        println("Checked the status of " + name.toUpperCase + ". Obtained: " + status.status))
      wentNiceClean <- client.use(_.clearStatus(HealthCheck(name)))
      _ <- Sync[F].delay(
        println("Cleaned " + name.toUpperCase + " status. Went ok?: " + wentNiceClean.ok))
      unknown <- client.use(_.check(HealthCheck(name)))
      _       <- Sync[F].delay(println("Current status of " + name.toUpperCase + ": " + unknown.status))
    } yield ()

  def settingAndFullClean(namesAndStatuses: List[(String, ServerStatus)]) = {

    for {
      _ <- Sync[F].delay(println("Setting services: " + namesAndStatuses))
      wentNiceSet <- namesAndStatuses.traverse(l =>
        client.use(_.setStatus(HealthStatus(HealthCheck(l._1), l._2))))
      allStatuses1 <- client.use(_.checkAll(EmptyInput()))
      _            <- Sync[F].delay(println("All statuses are: " + allStatuses1.all.mkString("\n")))
      _ <- Sync[F].delay(
        println("Went it ok in all cases? " + !wentNiceSet.contains(WentNice(false))))
      wentNiceClear <- client.use(_.cleanAll(EmptyInput()))
      _             <- Sync[F].delay(println("Went the cleaning part nice? " + wentNiceClear.ok))
      allStatuses2  <- client.use(_.checkAll(EmptyInput()))
      _             <- Sync[F].delay(println("All statuses are: " + allStatuses2.all.mkString("\n")))

    } yield ()
  }
}
