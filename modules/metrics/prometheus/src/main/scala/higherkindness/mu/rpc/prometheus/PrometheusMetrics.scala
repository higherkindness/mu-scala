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

package higherkindness.mu.rpc.prometheus

import cats.effect.Sync
import cats.syntax.functor._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import io.prometheus.client._
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.internal.metrics.MetricsOps._
import io.grpc.Status

case class PrometheusMetrics(
    activeCalls: Gauge,
    messagesSent: Counter,
    messagesReceived: Counter,
    headersTime: Histogram,
    totalTime: Histogram,
    methodTime: Histogram,
    statusTime: Histogram
)

object PrometheusMetrics {

  def build[F[_]: Sync](
      cr: CollectorRegistry,
      prefix: String = "higherkinderness.mu",
      classifier: Option[String]): F[MetricsOps[F]] =
    buildMetrics[F](prefix, classifier, cr).map(PrometheusMetrics[F])

  def apply[F[_]: Sync](metrics: PrometheusMetrics)(implicit F: Sync[F]): MetricsOps[F] =
    new MetricsOps[F] {
      override def increaseActiveCalls(
          methodInfo: GrpcMethodInfo,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.activeCalls.labels(label(classifier)).inc()
      }

      override def decreaseActiveCalls(
          methodInfo: GrpcMethodInfo,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.activeCalls.labels(label(classifier)).dec()
      }

      override def recordMessageSent(
          methodInfo: GrpcMethodInfo,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.messagesSent
          .labels(label(classifier), methodInfo.serviceName, methodInfo.methodName)
          .inc()
      }

      override def recordMessageReceived(
          methodInfo: GrpcMethodInfo,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.messagesReceived
          .labels(label(classifier), methodInfo.serviceName, methodInfo.methodName)
          .inc()
      }

      override def recordHeadersTime(
          methodInfo: GrpcMethodInfo,
          elapsed: Long,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.headersTime
          .labels(label(classifier))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
      }

      override def recordTotalTime(
          methodInfo: GrpcMethodInfo,
          status: Status,
          elapsed: Long,
          classifier: Option[String]): F[Unit] = F.delay {
        metrics.totalTime
          .labels(label(classifier))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
        metrics.methodTime
          .labels(label(classifier), methodTypeDescription(methodInfo))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
        metrics.statusTime
          .labels(label(classifier), statusDescription(grpcStatusFromRawStatus(status)))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
      }
    }

  private[this] def buildMetrics[F[_]: Sync](
      prefix: String,
      classifier: Option[String],
      registry: CollectorRegistry): F[PrometheusMetrics] = Sync[F].delay {
    PrometheusMetrics(
      activeCalls = Gauge
        .build()
        .name(s"${prefix}_active_calls")
        .help("Current active calls.")
        .labelNames("classifier")
        .register(registry),
      messagesSent = Counter
        .build()
        .name(s"${prefix}_messages_sent")
        .help("Number of messages sent by service and method.")
        .labelNames("classifier", "service", "method")
        .register(registry),
      messagesReceived = Counter
        .build()
        .name(s"${prefix}_messages_received")
        .help("Number of messages received by service and method.")
        .labelNames("classifier", "service", "method")
        .register(registry),
      headersTime = Histogram
        .build()
        .name(s"${prefix}_calls_header")
        .help("Accumulative time for header calls")
        .labelNames("classifier")
        .register(registry),
      totalTime = Histogram
        .build()
        .name(s"${prefix}_calls_total")
        .help("Total time for all calls")
        .labelNames("classifier")
        .register(registry),
      methodTime = Histogram
        .build()
        .name(s"${prefix}_calls_by_method")
        .help("Time for calls based on GRPC method")
        .labelNames("classifier", "method")
        .register(registry),
      statusTime = Histogram
        .build()
        .name(s"${prefix}_calls_by_status")
        .help("Time for calls based on GRPC status")
        .labelNames("classifier", "status")
        .register(registry)
    )
  }

  private[this] def label(classifier: Option[String]): String = classifier.getOrElse("")

}
