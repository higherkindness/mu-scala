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
package client

import java.time.{Clock, Instant}

import higherkindness.mu.rpc.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.prometheus.shared.Configuration
import io.grpc._

case class MonitoringClientInterceptor(clock: Clock)(implicit CFG: Configuration)
    extends ClientInterceptor {

  private[this] val clientMetrics: ClientMetrics = ClientMetrics(CFG)

  override def interceptCall[Req, Res](
      methodDescriptor: MethodDescriptor[Req, Res],
      callOptions: CallOptions,
      channel: Channel): ClientCall[Req, Res] = {

    val metricsMethod: ClientMetricsForMethod =
      ClientMetricsForMethod(GrpcMethodInfo(methodDescriptor), clientMetrics)

    MonitoringClientCall[Req, Res](
      channel.newCall(methodDescriptor, callOptions),
      clock,
      metricsMethod)
  }
}

object MonitoringClientInterceptor {

  def apply(implicit CFG: Configuration): MonitoringClientInterceptor =
    MonitoringClientInterceptor(Clock.systemDefaultZone)(CFG)

}

case class MonitoringClientCall[Req, Res](
    clientCall: ClientCall[Req, Res],
    clock: Clock,
    clientMetrics: ClientMetricsForMethod)(implicit CFG: Configuration)
    extends ForwardingClientCall.SimpleForwardingClientCall[Req, Res](clientCall) {

  override def start(responseListener: ClientCall.Listener[Res], headers: Metadata): Unit = {
    clientMetrics.recordCallStarted()
    val listener: MonitoringClientCallListener[Res] =
      MonitoringClientCallListener(responseListener, clientMetrics, clock)
    delegate().start(listener, headers)
  }

  override def sendMessage(requestMessage: Req): Unit = {
    if (clientMetrics.method.isClientStreaming) clientMetrics.recordStreamMessageSent()
    delegate().sendMessage(requestMessage)
  }
}

case class MonitoringClientCallListener[Res](
    delegate: ClientCall.Listener[Res],
    clientMetrics: ClientMetricsForMethod,
    clock: Clock)(implicit CFG: Configuration)
    extends ForwardingClientCallListener[Res] {

  private[this] val millisPerSecond       = 1000L
  private[this] val startInstant: Instant = clock.instant()

  override def onClose(status: Status, metadata: Metadata): Unit = {
    clientMetrics.recordClientHandled(status.getCode)
    if (CFG.isIncludeLatencyHistograms) {
      val latencySec = (clock.millis - startInstant.toEpochMilli) / millisPerSecond
        .asInstanceOf[Double]
      clientMetrics.recordLatency(latencySec)
    }
    delegate.onClose(status, metadata)
  }

  override def onMessage(responseMessage: Res): Unit = {
    if (clientMetrics.method.isServerStreaming) clientMetrics.recordStreamMessageReceived()
    delegate.onMessage(responseMessage)
  }
}
