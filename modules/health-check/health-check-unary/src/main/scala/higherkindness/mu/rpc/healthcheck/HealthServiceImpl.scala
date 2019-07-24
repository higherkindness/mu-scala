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

package higherkindness.mu.rpc.healthcheck

import cats.effect.{Async, Concurrent, Sync}
import cats.effect.concurrent.Ref
import cats.implicits._
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler
import service.HealthCheckServiceUnary
import higherkindness.mu.rpc.protocol.Empty

object HealthService {

  def buildInstance[F[_]: Sync: Concurrent](
      implicit s: Scheduler): F[HealthCheckServiceUnary[F]] = {

    val checkRef: F[Ref[F, Map[String, ServerStatus]]] =
      Ref.of[F, Map[String, ServerStatus]](Map.empty[String, ServerStatus])

    checkRef.map(c => new HealthCheckServiceUnaryImpl[F](c))
  }
}

abstract class AbstractHealthService[F[_]: Async](checkStatus: Ref[F, Map[String, ServerStatus]]) {

  def check(service: HealthCheck): F[ServerStatus] =
    checkStatus.modify(m => (m, m.getOrElse(service.nameService, ServerStatus("UNKNOWN"))))

  def clearStatus(service: HealthCheck): F[WentNice] =
    checkStatus.tryUpdate(_ - service.nameService).map(WentNice)

  def checkAll(empty: Empty.type): F[AllStatus] =
    checkStatus.get.map(m => m.keys.map(HealthCheck).toList.zip(m.values.toList)).map(AllStatus)

  def cleanAll(empty: Empty.type): F[WentNice] =
    checkStatus.tryUpdate(_ => Map.empty[String, ServerStatus]).map(WentNice)

}

class HealthCheckServiceUnaryImpl[F[_]: Async](checkStatus: Ref[F, Map[String, ServerStatus]])
    extends AbstractHealthService[F](checkStatus)
    with HealthCheckServiceUnary[F] {

  def setStatus(newStatus: HealthStatus): F[WentNice] =
    checkStatus
      .tryUpdate(_ + (newStatus.hc.nameService -> newStatus.status))
      .map(WentNice)
}
