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

import higherkindness.mu.rpc.interceptors.MetricType
import io.prometheus.client.{Counter, Histogram}

trait MetricsTypeSyntax {

  implicit final def metricsTypeSyntax(metricType: MetricType): MetricTypeOps =
    new MetricTypeOps(metricType)

}

final class MetricTypeOps(val metricType: MetricType) extends AnyVal {

  import metricType._

  def toCounterBuilder: Counter.Builder =
    Counter.build
      .namespace(namespace)
      .subsystem(subsystem.value)
      .name(name)
      .labelNames(labelNames: _*)
      .help(description)

  def toHistogramBuilder: Histogram.Builder =
    Histogram.build
      .namespace(namespace)
      .subsystem(subsystem.value)
      .name(name)
      .labelNames(labelNames: _*)
      .help(description)

}

object implicits extends MetricsTypeSyntax
