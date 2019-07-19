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
import higherkindness.mu.rpc.healthcheck.ServerStatus
import service.{AllStatuses, EmptyInput, HealthCheck, HealthCheckService, HealthStatus, WentNice}
import monix.reactive.Observable
import cats.implicits._

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
class HealthServiceImpl[F[_]: Sync](
    checkStatus: Ref[F, Map[String, ServerStatus]],
    watchRef: Ref[F, Map[String, Observable[ServerStatus]]])
    extends HealthCheckService[F] {

  override def check(service: HealthCheck): F[ServerStatus] =
    checkStatus.modify(m => (m, m.getOrElse(service.nameService, ServerStatus("UNKNOWN"))))

  override def setStatus(newStatus: HealthStatus): F[WentNice] =
    checkStatus.tryUpdate(_ + (newStatus.service.nameService -> newStatus.status)).map(WentNice)

  override def clearStatus(service: HealthCheck): F[WentNice] =
    checkStatus.tryUpdate(_ - service.nameService).map(WentNice)

  override def checkAll(empty: EmptyInput): F[service.AllStatuses] =
    checkStatus.get.map(m => m.keys.map(HealthCheck).toList.zip(m.values.toList)).map(AllStatuses)

  override def cleanAll(empty: EmptyInput): F[WentNice] =
    checkStatus.tryUpdate(_ => Map.empty[String, ServerStatus]).map(WentNice)

  //override def watch(service: String): Observable[ServerStatus] = {
  //   watchRef.get.map(_.getOrElse(service, Observable(ServerStatus("UNKNOWN"))))
  //}
}
