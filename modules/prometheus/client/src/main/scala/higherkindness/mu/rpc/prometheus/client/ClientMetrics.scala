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
package client

import higherkindness.mu.rpc.prometheus.shared.Configuration
import io.prometheus.client._

case class ClientMetrics(cfg: Configuration) {

  import higherkindness.mu.rpc.interceptors.metrics._
  import higherkindness.mu.rpc.prometheus.implicits._

  private[this] val rpcStartedBuilder: Counter.Builder =
    clientMetricRpcStarted(cfg.namespace).toCounterBuilder

  private[this] val rpcCompletedBuilder: Counter.Builder =
    clientMetricRpcCompleted(cfg.namespace).toCounterBuilder

  private[this] val completedLatencySecondsBuilder: Histogram.Builder =
    clientMetricCompletedLatencySeconds(cfg.namespace).toHistogramBuilder

  private[this] val streamMessagesReceivedBuilder: Counter.Builder =
    clientMetricStreamMessagesReceived(cfg.namespace).toCounterBuilder

  private[this] val streamMessagesSentBuilder: Counter.Builder =
    clientMetricStreamMessagesSent(cfg.namespace).toCounterBuilder

  val rpcStarted: Counter   = rpcStartedBuilder.register(cfg.collectorRegistry)
  val rpcCompleted: Counter = rpcCompletedBuilder.register(cfg.collectorRegistry)
  val streamMessagesReceived: Counter =
    streamMessagesReceivedBuilder.register(cfg.collectorRegistry)
  val streamMessagesSent: Counter = streamMessagesSentBuilder.register(cfg.collectorRegistry)

  val completedLatencySeconds: Option[Histogram] =
    if (cfg.isIncludeLatencyHistograms)
      Some(
        completedLatencySecondsBuilder
          .buckets(cfg.latencyBuckets: _*)
          .register(cfg.collectorRegistry))
    else None
}
