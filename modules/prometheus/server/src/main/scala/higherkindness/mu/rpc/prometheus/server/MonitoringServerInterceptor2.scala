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

package higherkindness.mu.rpc.prometheus.server

import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import io.grpc._

case class MetricsServerInterceptor() extends ServerInterceptor {

  override def interceptCall[Req, Res](
      call: ServerCall[Req, Res],
      requestHeaders: Metadata,
      next: ServerCallHandler[Req, Res]): ServerCall.Listener[Req] = {

    val method: GrpcMethodInfo = GrpcMethodInfo(call.getMethodDescriptor)

    val metricsCall: MetricsServerCall[Req, Res] =
      MetricsServerCall[Req, Res](call, method)

    MetricsServerCallListener[Req](next.startCall(metricsCall, requestHeaders), method)
  }
}

case class MetricsServerCall[Req, Res](serverCall: ServerCall[Req, Res], method: GrpcMethodInfo)
    extends ForwardingServerCall.SimpleForwardingServerCall[Req, Res](serverCall) {

  // Start time

  override def close(status: Status, responseHeaders: Metadata): Unit =
    // MetricsOps.recordTotalTime
    delegate().close(status, responseHeaders)

  override def sendMessage(message: Res): Unit =
    // MetricsOps.recordMessageSent
    delegate().sendMessage(message)
}

case class MetricsServerCallListener[Req](
    delegate: ServerCall.Listener[Req],
    method: GrpcMethodInfo)
    extends ForwardingServerCallListener[Req] {

  override def onMessage(request: Req): Unit =
    // MetricsOps.recordMessageReceived && MetricsOps.increaseActiveCalls
    delegate.onMessage(request)

  override def onComplete(): Unit =
    // MetricsOps.decreaseActiveCalls
    super.onComplete()
}
