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

package metricsops

import java.util.concurrent.TimeUnit

import cats.effect.Sync
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc.MethodDescriptor.MethodType._
import io.grpc.Status

object DropWizardMetricsOps {

  def apply[F[_]](registry: MetricRegistry, prefix: String = "higherkinderness.mu")(
    implicit F: Sync[F]) = new MetricsOps[F] {

    override def increaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(s"${classifiedName(prefix, classifier, methodInfo)}.active-calls").inc()
    }

    override def decreaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(s"${classifiedName(prefix, classifier, methodInfo)}.active-calls").dec()
    }

    override def recordMessageSent(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(eventName(methodInfo, classifier, "message-sent")).inc()
    }

    override def recordMessageReceived(methodInfo: GrpcMethodInfo, status: Status, classifier: Option[String]): F[Unit] =
      F.delay {
        registry.counter(eventName(methodInfo, classifier, "message-received")).inc()
      }

    override def recordHeadersTime(methodInfo: GrpcMethodInfo, elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
      registry.timer(eventName(methodInfo, classifier, "headers")).update(elapsed, TimeUnit.NANOSECONDS)
    }

    override def recordTotalTime(methodInfo: GrpcMethodInfo, status: Status, elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
      registry.timer(eventName(methodInfo, classifier, "total-time")).update(elapsed, TimeUnit.NANOSECONDS)
    }

    private def classifiedName(prefix: String, classifier: Option[String], methodInfo: GrpcMethodInfo) =
      classifier.map(c => s"$prefix.$c").getOrElse(s"$prefix.default") + s".${methodInfo.`type`}.${methodInfo.serviceName}.${methodInfo.methodName}"

    private def eventName(methodInfo: GrpcMethodInfo, classifier: Option[String], eventName: String): String = methodInfo.`type` match {
      case UNARY => s"${classifiedName(prefix, classifier, methodInfo)}.$eventName.unary"
      case CLIENT_STREAMING => s"${classifiedName(prefix, classifier, methodInfo)}.$eventName.client-streaming"
      case SERVER_STREAMING => s"${classifiedName(prefix, classifier, methodInfo)}.$eventName.server-streaming"
      case BIDI_STREAMING => s"${classifiedName(prefix, classifier, methodInfo)}.$eventName.bidi-streaming"
      case UNKNOWN => s"${classifiedName(prefix, classifier, methodInfo)}.$eventName.unknown"
    }
  }

}
