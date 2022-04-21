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

    val watchSignal: F[SignallingRef[F, ServiceStatus]] =
      SignallingRef.of[F, ServiceStatus](
        ServiceStatus("DummyService", HealthCheckResponse.ServingStatus.UNKNOWN)
      )

    for {
      ref <- checkRef
      sig <- watchSignal
    } yield new HealthServiceFS2Impl[F](ref, sig)
  }

}

class HealthServiceFS2Impl[F[_]: MonadThrow](
    checkRef: Ref[F, Map[String, ServingStatus]],
    watchSignal: SignallingRef[F, ServiceStatus]
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
      .filter(_.service === req.service)
      .map(_.status)

    (currentStatus ++ futureStatuses).changes
      .map(HealthCheckResponse(_))
      .pure[F]
  }

  def setStatus(serviceStatus: ServiceStatus): F[Unit] =
    for {
      _ <- checkRef.update(_ + (serviceStatus.service -> serviceStatus.status))
      _ <- watchSignal.set(serviceStatus)
    } yield ()

  def clearStatus(service: String): F[Unit] =
    checkRef.update(_ - service)

}
