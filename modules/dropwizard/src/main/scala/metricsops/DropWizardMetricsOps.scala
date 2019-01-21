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
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import MetricsOps._
import io.grpc.Status

object DropWizardMetricsOps {

  def apply[F[_]](registry: MetricRegistry, prefix: String = "higherkinderness.mu")(
      implicit F: Sync[F]) = new MetricsOps[F] {

    override def increaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(eventDescription(prefix, classifier, methodInfo, "active-calls")).inc()
    }

    override def decreaseActiveCalls(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(eventDescription(prefix, classifier, methodInfo, "active-calls")).dec()
    }

    override def recordMessageSent(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(eventDescription(prefix, classifier, methodInfo, "message-sent")).inc()
    }

    override def recordMessageReceived(
        methodInfo: GrpcMethodInfo,
        classifier: Option[String]): F[Unit] = F.delay {
      registry.counter(eventDescription(prefix, classifier, methodInfo, "message-received")).inc()
    }

    override def recordHeadersTime(
        methodInfo: GrpcMethodInfo,
        elapsed: Long,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .timer(eventDescription(prefix, classifier, methodInfo, "headers-time"))
        .update(elapsed, TimeUnit.NANOSECONDS)
    }

    override def recordTotalTime(
        methodInfo: GrpcMethodInfo,
        status: Status,
        elapsed: Long,
        classifier: Option[String]): F[Unit] = F.delay {
      registry
        .timer(eventDescription(prefix, classifier, methodInfo, "total-time", Some(status)))
        .update(elapsed, TimeUnit.NANOSECONDS)
    }

  }

}
