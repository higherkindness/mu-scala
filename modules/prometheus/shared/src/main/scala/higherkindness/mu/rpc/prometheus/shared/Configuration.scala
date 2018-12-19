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
package prometheus
package shared

import io.prometheus.client.CollectorRegistry

case class Configuration(
    isIncludeLatencyHistograms: Boolean,
    collectorRegistry: CollectorRegistry,
    latencyBuckets: Vector[Double],
    namespace: Option[String] = None) {

  def withCollectorRegistry(c: CollectorRegistry): Configuration =
    this.copy(collectorRegistry = c)

  def withLatencyBuckets(lb: Vector[Double]): Configuration =
    this.copy(latencyBuckets = lb)

  def withNamespace(namespace: String): Configuration = this.copy(namespace = Some(namespace))

}

object Configuration {

  private[this] val defaultLatencyBuckets: Vector[Double] =
    Vector[Double](.001, .005, .01, .05, 0.075, .1, .25, .5, 1, 2, 5, 10)

  def defaultBasicMetrics: Configuration =
    Configuration(
      isIncludeLatencyHistograms = false,
      CollectorRegistry.defaultRegistry,
      defaultLatencyBuckets)

  def defaultAllMetrics: Configuration =
    Configuration(
      isIncludeLatencyHistograms = true,
      CollectorRegistry.defaultRegistry,
      defaultLatencyBuckets)

}
