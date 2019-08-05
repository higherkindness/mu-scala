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

package higherkindness.mu.rpc.healthcheck.client.monix

import cats.effect.{Async, Resource}
import cats.implicits._
import monix.execution.Scheduler
import monix.reactive.Consumer
import higherkindness.mu.rpc.healthcheck.monix
import higherkindness.mu.rpc.healthcheck.unary.handler.{HealthCheck, HealthStatus, ServerStatus}
import higherkindness.mu.rpc.healthcheck.monix.serviceMonix.HealthCheckServiceMonix
import higherkindness.mu.rpc.protocol.Empty
import io.chrisdavenport.log4cats.Logger

class HealthCheckClientHandlerMonix[F[_]: Async](client: Resource[F, HealthCheckServiceMonix[F]])(
    implicit s: Scheduler,
    logger: Logger[F]) {

  def updatingWatching(name: String) =
    for {
      _      <- client.use(_.setStatus(HealthStatus(new HealthCheck(name), ServerStatus("SERVING"))))
      status <- client.use(_.check(new HealthCheck(name)))
      _      <- logger.info("Status of " + name + " service update to " + status)
    } yield ()

  def watching(name: String) = {
    val consumer = Consumer.foreach(println)
    client
      .use(
        _.watch(new HealthCheck(name))
          .consumeWith(consumer)
          .toAsync[F])
  }

  def settingAndCheck(name: String, status: ServerStatus) =
    for {
      _     <- logger.info("/////////////////////////////////UNARY")
      _     <- logger.info("UNARY: Is there some server named " + name.toUpperCase + "?")
      known <- client.use(_.check(new HealthCheck(name)))
      _     <- logger.info("UNARY: Actually the status is " + known.status)
      _ <- logger.info(
        "UNARY: Setting " + name.toUpperCase + " service with " + status.status + " status")
      _      <- client.use(_.setStatus(HealthStatus(new HealthCheck(name), status)))
      _      <- logger.info("UNARY: Added status: " + status.status + " to service: " + name.toUpperCase)
      status <- client.use(_.check(new HealthCheck(name)))
      _ <- logger.info(
        "UNARY: Checked the status of " + name.toUpperCase + ". Obtained: " + status.status)
      _       <- client.use(_.clearStatus(new HealthCheck(name)))
      unknown <- client.use(_.check(new HealthCheck(name)))
      _       <- logger.info("UNARY: Current status of " + name.toUpperCase + ": " + unknown.status)
    } yield ()

  def settingAndFullClean(namesAndStatuses: List[(String, ServerStatus)]) =
    for {
      _ <- logger.info("/////////////////////////////////UNARY ALL")
      _ <- logger.info("UNARY ALL: Setting services: " + namesAndStatuses)
      _ <- namesAndStatuses.traverse(l =>
        client.use(_.setStatus(HealthStatus(new HealthCheck(l._1), l._2))))
      allStatuses1 <- client.use(_.checkAll(Empty))
      _            <- logger.info("UNARY ALL: All statuses are: " + allStatuses1.all.mkString("\n"))
      _            <- client.use(_.cleanAll(Empty))
      allStatuses2 <- client.use(_.checkAll(Empty))
      _            <- logger.info("UNARY ALL: All statuses are: " + allStatuses2.all.mkString("\n"))

    } yield ()

}
