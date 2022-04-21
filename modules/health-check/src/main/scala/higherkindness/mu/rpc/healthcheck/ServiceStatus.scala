package higherkindness.mu.rpc.healthcheck

import grpc.health.v1.health.HealthCheckResponse.ServingStatus
import cats.kernel.Eq

final case class ServiceStatus(service: String, status: ServingStatus)

object ServiceStatus {

  implicit val eq: Eq[ServiceStatus] = Eq.fromUniversalEquals[ServiceStatus]

}
