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
package server

import freestyle.rpc.prometheus.shared.Configuration
import io.prometheus.client._

case class ServerMetrics(cfg: Configuration) {

  import ServerMetrics._

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

object ServerMetrics {

  val serverStartedBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("server")
    .name("started_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of RPCs started on the server.")

  val serverHandledBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("server")
    .name("handled_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method", "grpc_code")
    .help("Total number of RPCs completed on the server, regardless of success or failure.")

  val serverHandledLatencySecondsBuilder: Histogram.Builder = Histogram.build
    .namespace("grpc")
    .subsystem("server")
    .name("handled_latency_seconds")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Histogram of response latency (seconds) of gRPC that had been application-level handled by the server.")

  val serverStreamMessagesReceivedBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("server")
    .name("msg_received_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of stream messages received from the client.")

  val serverStreamMessagesSentBuilder: Counter.Builder = Counter.build
    .namespace("grpc")
    .subsystem("server")
    .name("msg_sent_total")
    .labelNames("grpc_type", "grpc_service", "grpc_method")
    .help("Total number of stream messages sent by the server.")
}
