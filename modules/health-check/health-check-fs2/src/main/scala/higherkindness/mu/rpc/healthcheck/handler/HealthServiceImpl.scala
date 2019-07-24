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
import higherkindness.mu.rpc.healthcheck.{service, ServerStatus}
import higherkindness.mu.rpc.healthcheck.service.{
  AllStatus,
  HealthCheck,
  HealthCheckService,
  HealthStatus,
  WentNice
}
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Topic
import higherkindness.mu.rpc.protocol.Empty

object HealthService {

  def buildInstance[F[_]: Sync: Concurrent]: F[HealthCheckService[F]] = {

    val checkRef: F[Ref[F, Map[String, ServerStatus]]] =
      Ref.of[F, Map[String, ServerStatus]](Map.empty[String, ServerStatus])

    val watchTopic: F[Topic[F, HealthStatus]] = Topic(
      HealthStatus(HealthCheck("FirstStatus"), ServerStatus("UNKNOWN")))

    for {
      c <- checkRef
      t <- watchTopic
    } yield new HealthServiceImpl[F](c, t)
  }
}
class HealthServiceImpl[F[_]: Async](
    checkStatus: Ref[F, Map[String, ServerStatus]],
    watchTopic: Topic[F, HealthStatus])
    extends HealthCheckService[F] {

  override def check(service: HealthCheck): F[ServerStatus] =
    checkStatus.modify(m => (m, m.getOrElse(service.nameService, ServerStatus("UNKNOWN"))))

  override def setStatus(newStatus: HealthStatus): F[WentNice] =
    checkStatus
      .tryUpdate(_ + (newStatus.hc.nameService -> newStatus.status))
      .map(WentNice) <*
      Stream.eval(watchTopic.publish1(newStatus)).compile.drain

  override def clearStatus(service: HealthCheck): F[WentNice] =
    checkStatus.tryUpdate(_ - service.nameService).map(WentNice)

  override def checkAll(empty: Empty.type): F[service.AllStatus] =
    checkStatus.get.map(m => m.keys.map(HealthCheck).toList.zip(m.values.toList)).map(AllStatus)

  override def cleanAll(empty: Empty.type): F[WentNice] =
    checkStatus.tryUpdate(_ => Map.empty[String, ServerStatus]).map(WentNice)

  override def watch(service: HealthCheck): Stream[F, HealthStatus] =
    watchTopic.subscribe(20).filter(hs => hs.hc == service)

}
/*
  //  val watchStream: F[Ref[F, Stream[F, ServerStatus]]] = Ref.of[F,Stream[F,ServerStatus]](Stream.empty)
  val watchObservable: Observable[HealthStatus] =
    Observable(HealthStatus(HealthCheck("exmaple"), ServerStatus("Serving")),
      HealthStatus(HealthCheck("notExmaple"), ServerStatus("Serving")))
 */
