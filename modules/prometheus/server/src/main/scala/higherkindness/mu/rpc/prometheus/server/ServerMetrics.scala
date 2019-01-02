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
package server

import higherkindness.mu.rpc.prometheus.shared.Configuration
import io.prometheus.client._

case class ServerMetrics(cfg: Configuration) {

  import higherkindness.mu.rpc.interceptors.metrics._
  import higherkindness.mu.rpc.prometheus.implicits._

  private[this] val serverStartedBuilder: Counter.Builder =
    serverMetricRpcStarted(cfg.namespace).toCounterBuilder

  private[this] val serverHandledBuilder: Counter.Builder =
    serverMetricHandledCompleted(cfg.namespace).toCounterBuilder

  private[this] val serverHandledLatencySecondsBuilder: Histogram.Builder =
    serverMetricHandledLatencySeconds(cfg.namespace).toHistogramBuilder

  private[this] val serverStreamMessagesReceivedBuilder: Counter.Builder =
    serverMetricStreamMessagesReceived(cfg.namespace).toCounterBuilder

  private[this] val serverStreamMessagesSentBuilder: Counter.Builder =
    serverMetricStreamMessagesSent(cfg.namespace).toCounterBuilder

  val serverStarted: Counter = serverStartedBuilder.register(cfg.collectorRegistry)

  val serverHandled: Counter = serverHandledBuilder.register(cfg.collectorRegistry)

  val serverStreamMessagesReceived: Counter =
    serverStreamMessagesReceivedBuilder.register(cfg.collectorRegistry)

  val serverStreamMessagesSent: Counter =
    serverStreamMessagesSentBuilder.register(cfg.collectorRegistry)

  val serverHandledLatencySeconds: Option[Histogram] =
    if (cfg.isIncludeLatencyHistograms)
      Some(
        serverHandledLatencySecondsBuilder
          .buckets(cfg.latencyBuckets: _*)
          .register(cfg.collectorRegistry))
    else None

}
