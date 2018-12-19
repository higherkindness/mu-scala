/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc
package dropwizard
package server

import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.prometheus.server.{
  BaseMonitorServerInterceptorTests,
  InterceptorsRuntime
}
import higherkindness.mu.rpc.prometheus.shared.Configuration
import io.prometheus.client.dropwizard.DropwizardExports

class MonitorServerInterceptorTests extends BaseMonitorServerInterceptorTests {

  override def name: String = "Dropwizard"

  override def namespace: Option[String] = Some("grpc")

  override def defaultInterceptorsRuntime: InterceptorsRuntime = {

    val metrics: MetricRegistry      = new MetricRegistry
    val configuration: Configuration = Configuration.defaultBasicMetrics
    configuration.collectorRegistry.register(new DropwizardExports(metrics))
    InterceptorsRuntime(configuration)
  }

  override def allMetricsInterceptorsRuntime: InterceptorsRuntime = {

    val metrics: MetricRegistry      = new MetricRegistry
    val configuration: Configuration = Configuration.defaultAllMetrics
    configuration.collectorRegistry.register(new DropwizardExports(metrics))
    InterceptorsRuntime(configuration)
  }

  override def interceptorsRuntimeWithNonDefaultBuckets(
      buckets: Vector[Double]): InterceptorsRuntime = {

    val metrics: MetricRegistry      = new MetricRegistry
    val configuration: Configuration = Configuration.defaultAllMetrics.withLatencyBuckets(buckets)
    configuration.collectorRegistry.register(new DropwizardExports(metrics))
    InterceptorsRuntime(configuration)
  }
}
