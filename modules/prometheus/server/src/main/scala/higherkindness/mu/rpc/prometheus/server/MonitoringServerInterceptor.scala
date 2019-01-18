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

import java.time.{Clock, Instant}

import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.prometheus.shared.Configuration
import io.grpc._

case class MonitoringServerInterceptor(clock: Clock)(implicit CFG: Configuration)
    extends ServerInterceptor {

  val serverMetrics: ServerMetrics = ServerMetrics(CFG)

  override def interceptCall[Req, Res](
      call: ServerCall[Req, Res],
      requestHeaders: Metadata,
      next: ServerCallHandler[Req, Res]): ServerCall.Listener[Req] = {

    val method: MethodDescriptor[Req, Res] = call.getMethodDescriptor
    val metricsMethod: ServerMetricsForMethod =
      ServerMetricsForMethod(GrpcMethodInfo(method), serverMetrics)

    val monitoringCall: MonitoringServerCall[Req, Res] =
      MonitoringServerCall[Req, Res](call, clock, metricsMethod)

    MonitoringServerCallListener[Req](next.startCall(monitoringCall, requestHeaders), metricsMethod)
  }
}

object MonitoringServerInterceptor {

  def apply(implicit CFG: Configuration): MonitoringServerInterceptor =
    MonitoringServerInterceptor(Clock.systemDefaultZone)(CFG)

}

case class MonitoringServerCall[Req, Res](
    serverCall: ServerCall[Req, Res],
    clock: Clock,
    serverMetrics: ServerMetricsForMethod)(implicit CFG: Configuration)
    extends ForwardingServerCall.SimpleForwardingServerCall[Req, Res](serverCall) {

  private[this] val millisPerSecond = 1000L

  private[this] val startInstant: Instant = clock.instant

  reportStartMetrics()

  override def close(status: Status, responseHeaders: Metadata): Unit = {
    reportEndMetrics(status)
    delegate().close(status, responseHeaders)
  }

  override def sendMessage(message: Res): Unit = {
    if (serverMetrics.method.isServerStreaming) serverMetrics.recordStreamMessageSent()
    delegate().sendMessage(message)
  }

  private[this] def reportStartMetrics(): Unit =
    serverMetrics.recordCallStarted()

  private[this] def reportEndMetrics(status: Status): Unit = {
    serverMetrics.recordServerHandled(status.getCode)
    if (CFG.isIncludeLatencyHistograms) {
      val latencySec = (clock.millis - startInstant.toEpochMilli) / millisPerSecond.toDouble
      serverMetrics.recordLatency(latencySec)
    }
  }
}

case class MonitoringServerCallListener[Req](
    delegate: ServerCall.Listener[Req],
    serverMetrics: ServerMetricsForMethod)
    extends ForwardingServerCallListener[Req] {

  override def onMessage(request: Req): Unit = {
    if (serverMetrics.method.isClientStreaming) serverMetrics.recordStreamMessageReceived()
    delegate.onMessage(request)
  }
}
