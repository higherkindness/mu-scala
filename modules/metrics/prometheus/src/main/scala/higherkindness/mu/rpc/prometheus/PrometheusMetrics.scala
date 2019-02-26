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
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import io.prometheus.client.{CollectorRegistry, Counter, Gauge, Summary}
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc.Status

case class PrometheusMetrics(
                              activeCalls: Gauge,
                              messagesSent: Counter,
                              messagesReceived: Counter,
                              headersTime: Summary,
                              totalTime: Summary
                            )

object PrometheusMetrics {

  def apply[F[_]](cr: CollectorRegistry, prefix: String = "higherkinderness.mu")(implicit F: Sync[F]) = F.delay {
    new MetricsOps[F] {
      override def increaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def decreaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordMessageSent(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordMessageReceived(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = ???

      override def recordHeadersTime(methodInfo: GrpcMethodInfo, elapsed: Long, classifier: Option[String]): F[Unit] = ???

      override def recordTotalTime(methodInfo: GrpcMethodInfo, status: Status, elapsed: Long, classifier: Option[String]): F[Unit] = ???
    }
  }

  def metrics(prefix: String, classifier: Option[String], registry: CollectorRegistry) = PrometheusMetrics(
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
        .help("Number of messages sent.")
        .labelNames("messages_sent")
        .register(registry),
    messagesReceived =
      Counter
        .build()
        .name(s"${prefix}_${classifier}_messages_received")
        .help("Number of messages received.")
        .labelNames("messages_received")
        .register(registry),
    headersTime =
      Summary
        .build()
        .name(s"${prefix}_${classifier}_calls_header")
        .help("Accumulative time for header calls")
        .labelNames("time", "header")
        .register(registry),
    totalTime =
      Summary
        .build()
        .name(s"${prefix}_${classifier}_calls_total")
        .help("Total time for all calls")
        .labelNames("time", "total")
        .register(registry)
  )

}
