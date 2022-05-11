/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

import cats._
import cats.effect._
import cats.syntax.all._
import _root_.fs2._
import _root_.fs2.concurrent._
import _root_.grpc.health.v1.health._
import _root_.grpc.health.v1.health.HealthCheckResponse.ServingStatus
import io.grpc.{Status, StatusException}

trait HealthService[F[_]] extends Health[F] {

  def setStatus(serviceStatus: ServiceStatus): F[Unit]

  def clearStatus(service: String): F[Unit]

}

object HealthService {

  def build[F[_]: Concurrent]: F[HealthService[F]] = {
    val checkRef: F[Ref[F, Map[String, ServingStatus]]] =
      // empty string indicates the health of the server in general,
      // rather than any particular gRPC service running on the server
      Ref.of[F, Map[String, ServingStatus]](Map("" -> ServingStatus.SERVING))

    val watchSignal: F[SignallingRef[F, Option[ServiceStatus]]] =
      SignallingRef.of[F, Option[ServiceStatus]](None)

    for {
      ref <- checkRef
      sig <- watchSignal
    } yield new HealthServiceFS2Impl[F](ref, sig)
  }

}

class HealthServiceFS2Impl[F[_]: MonadThrow](
    checkRef: Ref[F, Map[String, ServingStatus]],
    watchSignal: SignallingRef[F, Option[ServiceStatus]]
) extends HealthService[F] {

  private implicit val eqServingStatus: Eq[ServingStatus] =
    Eq.fromUniversalEquals

  private def getStatus(service: String): F[Option[ServingStatus]] =
    checkRef.get.map(_.get(service))

  def Check(req: HealthCheckRequest): F[HealthCheckResponse] =
    /*
     * https://github.com/grpc/grpc/blob/master/doc/health-checking.md
     *
     * "For each request received, if the service name can be found in the
     * registry, a response must be sent back with an OK status and the status
     * field should be set to SERVING or NOT_SERVING accordingly. If the
     * service name is not registered, the server returns a NOT_FOUND GRPC
     * status."
     */
    getStatus(req.service).flatMap {
      case Some(status) =>
        HealthCheckResponse(status).pure[F]
      case None =>
        MonadThrow[F].raiseError(new StatusException(Status.NOT_FOUND))
    }

  def Watch(req: HealthCheckRequest): F[Stream[F, HealthCheckResponse]] = {
    /*
     * https://github.com/grpc/grpc/blob/master/doc/health-checking.md
     *
     * "The server will immediately send back a message indicating the current
     * serving status. It will then subsequently send a new message whenever
     * the service's serving status changes."
     */
    val currentStatus =
      Stream.eval(getStatus(req.service).map(_.getOrElse(ServingStatus.SERVICE_UNKNOWN)))
    val futureStatuses = watchSignal.discrete
      .collect { case Some(x) => x }
      .filter(_.service === req.service)
      .map(_.status)

    (currentStatus ++ futureStatuses).changes
      .map(HealthCheckResponse(_))
      .pure[F]
  }

  def setStatus(serviceStatus: ServiceStatus): F[Unit] =
    for {
      _ <- checkRef.update(_ + (serviceStatus.service -> serviceStatus.status))
      _ <- watchSignal.set(Some(serviceStatus))
    } yield ()

  def clearStatus(service: String): F[Unit] =
    checkRef.update(_ - service)

}
