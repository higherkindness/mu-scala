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

package freestyle.rpc
package prometheus
package client

import freestyle.rpc.prometheus.shared.Configuration
import io.prometheus.client._

case class ClientMetrics(cfg: Configuration) {

  import ClientMetrics._

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

object ClientMetrics {

  val rpcStartedBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("client")
    .name("started_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of RPCs started on the client.")

  val rpcCompletedBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("client")
    .name("completed")
    .labelNames("grpc_type", "grpc_service", "grpc_method", "grpc_code")
    .help("Total number of RPCs completed on the client, regardless of success or failure.")

  val completedLatencySecondsBuilder: Histogram.Builder = Histogram.build
    .namespace("grpc")
    .subsystem("client")
    .name("completed_latency_seconds")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Histogram of rpc response latency (in seconds) for completed rpcs.")

  val streamMessagesReceivedBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("client")
    .name("msg_received_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of stream messages received from the server.")

  val streamMessagesSentBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("client")
    .name("msg_sent_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of stream messages sent by the client.")
}
