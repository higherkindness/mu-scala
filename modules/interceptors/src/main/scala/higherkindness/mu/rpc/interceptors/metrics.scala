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
package interceptors

sealed abstract class MetricsFor(val value: String) extends Product with Serializable
case object ServerMetrics                           extends MetricsFor(value = "server")
case object ClientMetrics                           extends MetricsFor(value = "client")

final case class MetricType(
    namespace: String,
    subsystem: MetricsFor,
    name: String,
    labelNames: List[String],
    description: String) {

  def toMetricString = s"${namespace}_${subsystem.value}_$name"

}

object metrics {

  // Namespaces:

  val namespace: String = "grpc"

  // Metrics names:

  private[this] val startedTotal: String            = "started_total"
  private[this] val handledTotal: String            = "handled_total"
  private[this] val handledLatencySeconds: String   = "handled_latency_seconds"
  private[this] val completed: String               = "completed"
  private[this] val completedLatencySeconds: String = "completed_latency_seconds"
  private[this] val msgReceivedTotal: String        = "msg_received_total"
  private[this] val msgSentTotal: String            = "msg_sent_total"

  // Labels:

  private[this] val grpcType: String    = "grpc_type"
  private[this] val grpcService: String = "grpc_service"
  private[this] val grpcMethod: String  = "grpc_method"
  private[this] val grpcCode: String    = "grpc_code"

  // Misc:

  private[this] val baseLabels: List[String] = List(grpcType, grpcService, grpcMethod)

  // Client - Predefined metrics:

  def clientMetricRpcStarted(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ClientMetrics,
      startedTotal,
      baseLabels,
      "Total number of RPCs started on the client.")
  def clientMetricRpcCompleted(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ClientMetrics,
      completed,
      baseLabels :+ grpcCode,
      "Total number of RPCs completed on the client, regardless of success or failure."
    )
  def clientMetricCompletedLatencySeconds(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ClientMetrics,
      completedLatencySeconds,
      baseLabels,
      "Histogram of rpc response latency (in seconds) for completed rpcs.")
  def clientMetricStreamMessagesReceived(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ClientMetrics,
      msgReceivedTotal,
      baseLabels,
      "Total number of stream messages received from the server.")
  def clientMetricStreamMessagesSent(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ClientMetrics,
      msgSentTotal,
      baseLabels,
      "Total number of stream messages sent by the client.")

  // Server - Predefined metrics:

  def serverMetricRpcStarted(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ServerMetrics,
      startedTotal,
      baseLabels,
      "Total number of RPCs started on the server.")
  def serverMetricHandledCompleted(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ServerMetrics,
      handledTotal,
      baseLabels :+ grpcCode,
      "Total number of RPCs completed on the server, regardless of success or failure."
    )
  def serverMetricHandledLatencySeconds(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ServerMetrics,
      handledLatencySeconds,
      baseLabels,
      "Histogram of response latency (seconds) of gRPC that had been application-level handled by the server."
    )
  def serverMetricStreamMessagesReceived(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ServerMetrics,
      msgReceivedTotal,
      baseLabels,
      "Total number of stream messages received from the client.")
  def serverMetricStreamMessagesSent(nsp: Option[String]): MetricType =
    MetricType(
      nsp.getOrElse(namespace),
      ServerMetrics,
      msgSentTotal,
      baseLabels,
      "Total number of stream messages sent by the server.")
}
