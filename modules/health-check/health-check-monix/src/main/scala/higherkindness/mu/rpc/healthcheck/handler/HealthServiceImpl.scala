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

import cats.effect.{Async, Concurrent, Sync}
import cats.effect.concurrent.Ref
import higherkindness.mu.rpc.healthcheck._
import cats.implicits._
import service.HealthCheckServiceMonix
import monix.execution.Scheduler
import monix.reactive.{MulticastStrategy, Observable, Observer, Pipe}

object HealthService {

  def buildInstance[F[_]: Sync: Concurrent](
      implicit s: Scheduler): F[HealthCheckServiceMonix[F]] = {

    val checkRef: F[Ref[F, Map[String, ServerStatus]]] =
      Ref.of[F, Map[String, ServerStatus]](Map.empty[String, ServerStatus])

    val pipe: (Observer.Sync[HealthStatus], Observable[HealthStatus]) =
      Pipe(
        MulticastStrategy.behavior(
          HealthStatus(HealthCheck("FirstStatus"), ServerStatus("UNKNOWN"))))
        .concurrent(s)

    checkRef.map(c => new HealthCheckServiceMonixImpl[F](c, pipe))
  }
}

class HealthCheckServiceMonixImpl[F[_]: Async](
    checkStatus: Ref[F, Map[String, ServerStatus]],
    pipe: (Observer.Sync[HealthStatus], Observable[HealthStatus]))(implicit s: Scheduler)
    extends AbstractHealthService[F](checkStatus)
    with HealthCheckServiceMonix[F] {

  override def setStatus(newStatus: HealthStatus): F[WentNice] =
    checkStatus
      .tryUpdate(_ + (newStatus.hc.nameService -> newStatus.status))
      .map(WentNice) <*
      Async[F].delay(pipe._1.onNext(newStatus))

  override def watch(service: HealthCheck): Observable[HealthStatus] =
    pipe._2.filter(hs => hs.hc == service)

}
