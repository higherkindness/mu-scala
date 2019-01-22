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

/*
 * [[MetricsOps]] algebra able to record Dropwizard metrics.
 *
 * The list of registered metrics contains:
 *
 * {prefix}.{classifier}.{serviceName}.{methodName}.{methodType}.active-calls - Counter
 * {prefix}.{classifier}.{serviceName}.{methodName}.{methodType}.messages-sent - Counter
 * {prefix}.{classifier}.{serviceName}.{methodName}.{methodType}.messages-received - Counter
 * {prefix}.{classifier}.{serviceName}.{methodName}.{methodType}.headers-time - Timer
 * {prefix}.{classifier}.{serviceName}.{methodName}.{methodType}.total-time.{status} - Timer
 *
 * {methodType} can be one of the following: "unary", "client-streaming", "server-streaming", "bidi-streaming", "unknown"
 * {status} can be one of the following:
 *    - "ok"
 *    - "cancelled"
 *    - "deadline-exceeded"
 *    - "internal"
 *    - "resource-exhausted"
 *    - "unauthenticated"
 *    - "unavailable"
 *    - "unimplemented"
 *    - "unknown"
 *    - "unreachable-error"
 *
 */

package metricsops

import java.util.concurrent.TimeUnit

import cats.effect.Sync
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.internal.metrics.MetricsOps._
import io.grpc.MethodDescriptor.MethodType._
import io.grpc.Status

object DropWizardMetricsOps {

  sealed trait GaugeType
  case object Timer   extends GaugeType
  case object Counter extends GaugeType

  def apply[F[_]](registry: MetricRegistry, prefix: String = "higherkinderness.mu")(
      implicit F: Sync[F]) = new MetricsOps[F] {

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
          s"${prefixDefinition(prefix, classifier)}.${methodInfo.serviceName}.${methodInfo.methodName}.messages-sent")
        .inc()
    }

    override def recordMessageReceived(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .counter(
          s"${prefixDefinition(prefix, classifier)}.${methodInfo.serviceName}.${methodInfo.methodName}.messages-received")
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
        .timer(
          s"${prefixDefinition(prefix, classifier)}.${methodTypeDescription(methodInfo)}.calls")
        .update(elapsed, TimeUnit.NANOSECONDS)

      registry
        .timer(
          s"${prefixDefinition(prefix, classifier)}.${statusDescription(grpcStatusFromRawStatus(status))}.calls")
        .update(elapsed, TimeUnit.NANOSECONDS)
    }

  }

  private def prefixDefinition(prefix: String, classifier: Option[String]) =
    classifier
      .map(c => s"$prefix.$c")
      .getOrElse(s"$prefix.default")

  def methodTypeDescription(methodInfo: GrpcMethodInfo): String =
    methodInfo.`type` match {
      case UNARY            => "unary"
      case CLIENT_STREAMING => "client-streaming"
      case SERVER_STREAMING => "server-streaming"
      case BIDI_STREAMING   => "bidi-streaming"
      case UNKNOWN          => "unknown"
    }

  def statusDescription(status: GrpcStatus): String = status match {
    case OK                => "ok"
    case Cancelled         => "cancelled"
    case DeadlineExceeded  => "deadline-exceeded"
    case Internal          => "internal"
    case ResourceExhausted => "resource-exhausted"
    case Unauthenticated   => "unauthenticated"
    case Unavailable       => "unavailable"
    case Unimplemented     => "unimplemented"
    case Unknown           => "unknown"
    case _                 => "unreachable-error"
  }

}
