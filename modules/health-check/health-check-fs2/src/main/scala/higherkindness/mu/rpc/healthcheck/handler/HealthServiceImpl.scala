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
import higherkindness.mu.rpc.healthcheck.service.HealthCheckServiceFS2

import cats.implicits._
import fs2.Stream
import fs2.concurrent.Topic

object HealthServiceFS2 {

  def buildInstance[F[_]: Sync: Concurrent]: F[HealthCheckServiceFS2[F]] = {

    val checkRef: F[Ref[F, Map[String, ServerStatus]]] =
      Ref.of[F, Map[String, ServerStatus]](Map.empty[String, ServerStatus])

    val watchTopic: F[Topic[F, HealthStatus]] = Topic(
      HealthStatus(new HealthCheck("FirstStatus"), ServerStatus("UNKNOWN")))

    for {
      c <- checkRef
      t <- watchTopic
    } yield new HealthCheckServiceFS2Impl[F](c, t)
  }
}
class HealthCheckServiceFS2Impl[F[_]: Async](
    checkStatus: Ref[F, Map[String, ServerStatus]],
    watchTopic: Topic[F, HealthStatus])
    extends AbstractHealthService[F](checkStatus)
    with HealthCheckServiceFS2[F] {

  def setStatus(newStatus: HealthStatus): F[Unit] =
    checkStatus
      .update(_ + (newStatus.hc.nameService -> newStatus.status)) <*
      Stream.eval(watchTopic.publish1(newStatus)).compile.drain

  def watch(service: HealthCheck): Stream[F, HealthStatus] =
    watchTopic.subscribe(20).filter(hs => hs.hc == service)

}
