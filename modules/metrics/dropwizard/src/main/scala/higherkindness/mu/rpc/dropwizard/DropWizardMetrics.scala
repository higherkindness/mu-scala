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

package higherkindness.mu.rpc.dropwizard

import java.util.concurrent.TimeUnit

import cats.effect.Sync
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.internal.metrics.MetricsOps._
import io.grpc.Status

/*
 * [[MetricsOps]] algebra able to record Dropwizard metrics.
 *
 * The list of registered metrics contains:
 *
 * {prefix}.{classifier}.active.calls - Counter
 * {prefix}.{classifier}.{serviceName}.{methodName}.messages.sent - Counter
 * {prefix}.{classifier}.{serviceName}.{methodName}.messages.received - Counter
 * {prefix}.{classifier}.calls.header - Timer
 * {prefix}.{classifier}.calls.total - Timer
 * {prefix}.{classifier}.{methodType} - Timer
 * {prefix}.{classifier}.{status} - Timer
 *
 * {methodType} can be one of the following:
 *    - "unary"
 *    - "client-streaming"
 *    - "server-streaming"
 *    - "bidi-streaming"
 *    - "unknown"
 *
 * {status} can be one of the following:
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
 */
object DropWizardMetrics {

  sealed trait GaugeType
  case object Timer   extends GaugeType
  case object Counter extends GaugeType

  def apply[F[_]](registry: MetricRegistry, prefix: String = "higherkinderness.mu")(
      implicit F: Sync[F]): MetricsOps[F] = new MetricsOps[F] {

    override def increaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(s"${prefixDefinition(prefix, classifier)}.active.calls").inc()
    }

    override def decreaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(s"${prefixDefinition(prefix, classifier)}.active.calls").dec()
    }

    override def recordMessageSent(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .counter(
          s"${prefixDefinition(prefix, classifier)}.${methodInfo.serviceName}.${methodInfo.methodName}.messages.sent")
        .inc()
    }

    override def recordMessageReceived(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .counter(
          s"${prefixDefinition(prefix, classifier)}.${methodInfo.serviceName}.${methodInfo.methodName}.messages.received")
        .inc()
    }

    override def recordHeadersTime(
        methodInfo: GrpcMethodInfo,
        elapsed: Long,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .timer(s"${prefixDefinition(prefix, classifier)}.calls.header")
        .update(elapsed, TimeUnit.NANOSECONDS)
    }

    override def recordTotalTime(
        methodInfo: GrpcMethodInfo,
        status: Status,
        elapsed: Long,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .timer(s"${prefixDefinition(prefix, classifier)}.calls.total")
        .update(elapsed, TimeUnit.NANOSECONDS)

      registry
        .timer(s"${prefixDefinition(prefix, classifier)}.${methodTypeDescription(methodInfo)}")
        .update(elapsed, TimeUnit.NANOSECONDS)

      registry
        .timer(
          s"${prefixDefinition(prefix, classifier)}.${statusDescription(grpcStatusFromRawStatus(status))}")
        .update(elapsed, TimeUnit.NANOSECONDS)
    }

  }

  private def prefixDefinition(prefix: String, classifier: Option[String]) =
    classifier
      .map(c => s"$prefix.$c")
      .getOrElse(s"$prefix.default")

}
