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

package higherkindness.mu.rpc
package prometheus
package shared

import higherkindness.mu.rpc.interceptors.MetricType
import io.prometheus.client._

import scala.collection.JavaConverters._

object RegistryHelper {

  def findRecordedMetric(mt: MetricType)(
      implicit CR: CollectorRegistry): Option[Collector.MetricFamilySamples] =
    CR.metricFamilySamples.asScala.find(_.name == mt.toMetricString)

  def findRecordedMetricOrThrow(mt: MetricType)(
      implicit CR: CollectorRegistry): Collector.MetricFamilySamples =
    findRecordedMetric(mt).getOrElse(
      throw new IllegalArgumentException(s"Could not find metric with name: ${mt.toMetricString}"))

  def extractMetricValue(mt: MetricType)(implicit CR: CollectorRegistry): Double = {
    val result = findRecordedMetricOrThrow(mt)
    result.samples.asScala.headOption
      .map(_.value)
      .getOrElse(throw new IllegalArgumentException(
        s"Expected one value, but got ${result.samples.size} for metric ${mt.toMetricString}"))
  }

  def countSamples(mt: MetricType, sampleName: String)(implicit CR: CollectorRegistry): Int =
    CR.metricFamilySamples.asScala
      .filter(_.name == mt.toMetricString)
      .map { sample =>
        sample.samples.asScala.count(_.name == sampleName)
      }
      .sum

  def printRegistry(collectorRegistry: CollectorRegistry): Unit =
    collectorRegistry.metricFamilySamples.asScala.foreach(println)

}
