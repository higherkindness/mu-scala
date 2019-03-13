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

/*
 * [[MetricsOps]] algebra able to record Prometheus metrics.
 *
 * The list of registered metrics contains:
 *
 * {prefix}_active_calls{labels=classifier} - Counter
 * {prefix}_messages_sent{labels=classifier,service,method} - Counter
 * {prefix}_messages_received{labels=classifier,service,method} - Counter
 * {prefix}_calls_header{labels=classifier} - Histogram
 * {prefix}_calls_total{labels=classifier,method,status} - Histogram
 *
 * `method` can be one of the following:
 *    - "unary-methods"
 *    - "client-streaming-methods"
 *    - "server-streaming-methods"
 *    - "bidi-streaming-methods"
 *    - "unknown-methods"
 *
 * `status` can be one of the following:
 *    - "ok-statuses"
 *    - "cancelled-statuses"
 *    - "deadline-exceeded-statuses"
 *    - "internal-statuses"
 *    - "resource-exhausted-statuses"
 *    - "unauthenticated-statuses"
 *    - "unavailable-statuses"
 *    - "unimplemented-statuses"
 *    - "unknown-statuses"
 *    - "unreachable-error-statuses"
 *
 */
object PrometheusMetrics {

  private[this] case class Metrics(
      activeCalls: Gauge,
      messagesSent: Counter,
      messagesReceived: Counter,
      headersTime: Histogram,
      totalTime: Histogram
  )

  def build[F[_]: Sync](
      cr: CollectorRegistry,
      prefix: String = "higherkinderness.mu"): F[MetricsOps[F]] =
    buildMetrics[F](prefix, cr).map(PrometheusMetrics[F])

  def apply[F[_]: Sync](metrics: Metrics)(implicit F: Sync[F]): MetricsOps[F] =
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
          .labels(
            label(classifier),
            methodTypeDescription(methodInfo),
            statusDescription(grpcStatusFromRawStatus(status)))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
      }
    }

  private[this] def buildMetrics[F[_]: Sync](
      prefix: String,
      registry: CollectorRegistry): F[Metrics] = Sync[F].delay {
    Metrics(
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
        .labelNames("classifier", "method", "status")
        .register(registry)
    )
  }

  private[this] def label(classifier: Option[String]): String = classifier.getOrElse("")

}
