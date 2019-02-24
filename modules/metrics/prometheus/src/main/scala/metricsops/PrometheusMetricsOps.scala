package metricsops

import cats.effect.Sync
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc.Status
import io.prometheus.client._

object PrometheusMetricsOps {

  def apply[F[_]](registry: CollectorRegistry, prefix: String)(implicit F: Sync[F]) = F.delay {
    new MetricsOps[F] {
      override def increaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def decreaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordMessageSent(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordMessageReceived(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordHeadersTime(methodInfo: GrpcMethodInfo, elapsed: Long, classifier: Option[String]): F[Unit] = ???

      override def recordTotalTime(methodInfo: GrpcMethodInfo, status: Status, elapsed: Long, classifier: Option[String]): F[Unit] = ???
    }
  }

  private[this] def metrics(prefix: String, classifier: Option[String], registry: CollectorRegistry) = PrometheusMetrics(
    activeCalls = Gauge
      .build()
      .name(s"${prefix}_${classifier}_active_calls")
      .help("Current active calls.")
      .labelNames("active_calls")
      .register(registry),
    messagesSent =
      Counter
      .build()
      .name(s"${prefix}_${classifier}_messages_sent")
  )


  case class PrometheusMetrics(
                                activeCalls: Gauge,
                                messagesSent: Counter,
                                messagesReceived: Counter,
                                headersTime: Summary,
                                totalTime: Summary
                              )

  def test(metrics: PrometheusMetrics) = metrics.activeCalls.
}
