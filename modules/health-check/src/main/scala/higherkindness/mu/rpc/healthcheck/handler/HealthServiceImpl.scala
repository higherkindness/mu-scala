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

package higherkindness.mu.rpc.healthcheck.handler

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import higherkindness.mu.rpc.healthcheck.{ServerStatus, UNKNOWN}
import service.{HealthCheck, HealthCheckService, HealthStatus}
import monix.reactive.Observable

object HealthService {

  def buildInstance[F[_]: Sync]: F[HealthCheckService[F]] = {
    val checkRef: F[Ref[F, Map[String, ServerStatus]]] =
      Ref.of[F, Map[String, ServerStatus]](Map.empty[String, ServerStatus])
    val watchRef: F[Ref[F, Map[String, Observable[ServerStatus]]]] =
      Ref.of[F, Map[String, Observable[ServerStatus]]](Map.empty[String, Observable[ServerStatus]])

    for {
      c <- checkRef
      w <- watchRef
    } yield new HealthServiceImpl[F](c, w)

  }
}
class HealthServiceImpl[F[_]](
    checkStatus: Ref[F, Map[String, ServerStatus]],
    watchRef: Ref[F, Map[String, Observable[ServerStatus]]])
    extends HealthCheckService[F] {

  override def check(service: HealthCheck): F[ServerStatus] =
    checkStatus.modify(m => (m, m.getOrElse(service.nameService, UNKNOWN)))

  override def setStatus(newStatus: HealthStatus): F[Boolean] =
    checkStatus.tryUpdate(_ + (newStatus.service.nameService -> newStatus.status))

  // def watch(service: String): Observable[Boolean] = {
  //   refStatus.set(map => map :+ ("myService", false))
  // }
}
