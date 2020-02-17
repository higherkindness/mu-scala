/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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
import cats.implicits._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.internal.metrics.MetricsOps._
import io.grpc.Status
import io.prometheus.client._

/*
 * [[MetricsOps]] algebra able to record Prometheus metrics.
 *
 * The list of registered metrics contains:
 *
 * {prefix}_active_calls{labels=classifier} - Counter
 * {prefix}_messages_sent{labels=classifier,service,method} - Counter
 * {prefix}_messages_received{labels=classifier,service,method} - Counter
 * {prefix}_calls_header{labels=classifier} - Histogram
 * {prefix}_calls_total{labels=classifier,methodType,status} - Histogram
 *
 * `methodType` can be one of the following:
 *    - "unary"
 *    - "client-streaming"
 *    - "server-streaming"
 *    - "bidi-streaming"
 *    - "unknown"
 *
 * `status` can be one of the following:
 *    - "ok"
 *    - "cancelled"
 *    - "deadline-exceeded"
 *    - "internal"
 *    - "resource-exhausted"
 *    - "unauthenticated"
 *    - "unavailable"
 *    - "unimplemented"
 *    - "unknown-status"
 *    - "unreachable-error"
 *
 * The `buildFullTotal` method contains the following metrics:
 *
 * {prefix}_active_calls{labels=classifier} - Counter
 * {prefix}_messages_sent{labels=classifier,service,method} - Counter
 * {prefix}_messages_received{labels=classifier,service,method} - Counter
 * {prefix}_calls_total{labels=classifier,service,method,status} - Histogram
 */
object PrometheusMetrics {

  case class Metrics(
      activeCalls: Option[Gauge],
      messagesSent: Option[Counter],
      messagesReceived: Option[Counter],
      headersTime: Option[Histogram],
      totalTime: Option[Histogram]
  )

  def build[F[_]: Sync](
      cr: CollectorRegistry,
      prefix: String = "higherkindness_mu"
  ): F[MetricsOps[F]] =
    buildMetrics[F](prefix, cr).map(new DefaultPrometheusMetricsOps[F](_))

  def apply[F[_]: Sync](metrics: Metrics): MetricsOps[F] =
    new DefaultPrometheusMetricsOps(metrics)

  def buildFullTotal[F[_]: Sync](
      cr: CollectorRegistry,
      prefix: String = "higherkindness_mu"
  ): F[MetricsOps[F]] =
    buildFullTotalMetrics[F](prefix, cr).map { metrics =>
      new DefaultPrometheusMetricsOps[F](metrics) {
        override def recordTotalTime(
            methodInfo: GrpcMethodInfo,
            status: Status,
            elapsed: Long,
            classifier: Option[String]
        ): F[Unit] =
          metrics.totalTime.fold(().pure[F])(hist =>
            Sync[F].delay(
              hist
                .labels(
                  label(classifier),
                  methodInfo.serviceName,
                  methodInfo.methodName,
                  statusDescription(grpcStatusFromRawStatus(status))
                )
                .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
            )
          )
      }
    }

  class DefaultPrometheusMetricsOps[F[_]](metrics: Metrics)(implicit F: Sync[F])
      extends MetricsOps[F] {
    override def increaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]
    ): F[Unit] = opDelay(metrics.activeCalls)(_.labels(label(classifier)).inc())

    override def decreaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]
    ): F[Unit] = opDelay(metrics.activeCalls)(_.labels(label(classifier)).dec())

    override def recordMessageSent(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]
    ): F[Unit] =
      opDelay(metrics.messagesSent)(
        _.labels(label(classifier), methodInfo.serviceName, methodInfo.methodName).inc()
      )

    override def recordMessageReceived(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]
    ): F[Unit] =
      opDelay(metrics.messagesReceived)(
        _.labels(label(classifier), methodInfo.serviceName, methodInfo.methodName).inc()
      )

    override def recordHeadersTime(
        methodInfo: GrpcMethodInfo,
        elapsed: Long,
        classifier: Option[String]
    ): F[Unit] =
      opDelay(metrics.headersTime)(
        _.labels(label(classifier))
          .observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
      )

    override def recordTotalTime(
        methodInfo: GrpcMethodInfo,
        status: Status,
        elapsed: Long,
        classifier: Option[String]
    ): F[Unit] =
      opDelay(metrics.totalTime)(
        _.labels(
          label(classifier),
          methodTypeDescription(methodInfo),
          statusDescription(grpcStatusFromRawStatus(status))
        ).observe(SimpleTimer.elapsedSecondsFromNanos(0, elapsed))
      )

    private[this] def opDelay[A](maybe: Option[A])(f: A => Unit): F[Unit] =
      maybe.fold(().pure[F])(a => F.delay(f(a)))
  }

  def defaultActiveCalls(prefix: String, registry: CollectorRegistry): Gauge =
    Gauge
      .build()
      .name(s"${prefix}_active_calls")
      .help("Current active calls.")
      .labelNames("classifier")
      .register(registry)

  def defaultMessageSent(prefix: String, registry: CollectorRegistry): Counter =
    Counter
      .build()
      .name(s"${prefix}_messages_sent")
      .help("Number of messages sent by service and method.")
      .labelNames("classifier", "service", "method")
      .register(registry)

  def defaultMessageReceived(prefix: String, registry: CollectorRegistry): Counter =
    Counter
      .build()
      .name(s"${prefix}_messages_received")
      .help("Number of messages received by service and method.")
      .labelNames("classifier", "service", "method")
      .register(registry)

  def defaultHeadersTime(prefix: String, registry: CollectorRegistry): Histogram =
    Histogram
      .build()
      .name(s"${prefix}_calls_header")
      .help("Accumulative time for header calls")
      .labelNames("classifier")
      .register(registry)

  def defaultTotalTime(prefix: String, registry: CollectorRegistry): Histogram =
    Histogram
      .build()
      .name(s"${prefix}_calls_total")
      .help("Total time for all calls")
      .labelNames("classifier", "method", "status")
      .register(registry)

  def fullTotalTime(prefix: String, registry: CollectorRegistry): Histogram =
    Histogram
      .build()
      .name(s"${prefix}_calls_total")
      .help("Total time for all calls")
      .labelNames("classifier", "service", "method", "status")
      .register(registry)

  private[this] def buildMetrics[F[_]: Sync](
      prefix: String,
      registry: CollectorRegistry
  ): F[Metrics] = Sync[F].delay {
    Metrics(
      activeCalls = defaultActiveCalls(prefix, registry).some,
      messagesSent = defaultMessageSent(prefix, registry).some,
      messagesReceived = defaultMessageReceived(prefix, registry).some,
      headersTime = defaultHeadersTime(prefix, registry).some,
      totalTime = defaultTotalTime(prefix, registry).some
    )
  }

  private[this] def buildFullTotalMetrics[F[_]: Sync](
      prefix: String,
      registry: CollectorRegistry
  ): F[Metrics] = Sync[F].delay {
    Metrics(
      activeCalls = defaultActiveCalls(prefix, registry).some,
      messagesSent = defaultMessageSent(prefix, registry).some,
      messagesReceived = defaultMessageReceived(prefix, registry).some,
      headersTime = None,
      totalTime = fullTotalTime(prefix, registry).some
    )
  }

  private[this] def label(classifier: Option[String]): String = classifier.getOrElse("")

}
