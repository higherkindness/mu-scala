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
package channel.metrics

import cats.effect.{Clock, Effect}
import cats.implicits._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc._

import scala.concurrent.duration._

case class MetricsChannelInterceptor[F[_]: Effect: Clock](
    metricsOps: MetricsOps[F],
    classifier: Option[String])
    extends ClientInterceptor {

  override def interceptCall[Req, Res](
      methodDescriptor: MethodDescriptor[Req, Res],
      callOptions: CallOptions,
      channel: Channel): ClientCall[Req, Res] = {

    val methodInfo: GrpcMethodInfo = GrpcMethodInfo(methodDescriptor)

    MetricsClientCall[F, Req, Res](
      channel.newCall(methodDescriptor, callOptions),
      methodInfo,
      metricsOps,
      classifier)
  }
}

case class MetricsClientCall[F[_]: Clock, Req, Res](
    clientCall: ClientCall[Req, Res],
    methodInfo: GrpcMethodInfo,
    metricsOps: MetricsOps[F],
    classifier: Option[String])(implicit E: Effect[F])
    extends ForwardingClientCall.SimpleForwardingClientCall[Req, Res](clientCall) {

  override def start(responseListener: ClientCall.Listener[Res], headers: Metadata): Unit =
    E.toIO {
      for {
        _ <- metricsOps.increaseActiveCalls(methodInfo, classifier)
        listener <- MetricsChannelCallListener.build[F, Res](
          delegate = responseListener,
          methodInfo = methodInfo,
          metricsOps = metricsOps,
          classifier = classifier)
        st <- E.delay(delegate.start(listener, headers))
      } yield st
    }.unsafeRunSync

  override def sendMessage(requestMessage: Req): Unit =
    E.toIO {
      metricsOps.recordMessageSent(methodInfo, classifier) *>
        E.delay(delegate.sendMessage(requestMessage))
    }.unsafeRunSync
}

class MetricsChannelCallListener[F[_], Res](
    val delegate: ClientCall.Listener[Res],
    methodInfo: GrpcMethodInfo,
    metricsOps: MetricsOps[F],
    startTime: Long,
    classifier: Option[String])(implicit E: Effect[F], C: Clock[F])
    extends ForwardingClientCallListener[Res] {

  override def onHeaders(headers: Metadata): Unit =
    E.toIO {
      for {
        now <- C.monotonic(NANOSECONDS)
        _   <- metricsOps.recordHeadersTime(methodInfo, now - startTime, classifier)
        onH <- E.delay(delegate.onHeaders(headers))
      } yield onH
    }.unsafeRunSync

  override def onMessage(responseMessage: Res): Unit =
    E.toIO {
      metricsOps.recordMessageReceived(methodInfo, classifier) *>
        E.delay(delegate.onMessage(responseMessage))
    }.unsafeRunSync

  override def onClose(status: Status, metadata: Metadata): Unit =
    E.toIO {
      for {
        now <- C.monotonic(NANOSECONDS)
        _   <- metricsOps.recordTotalTime(methodInfo, status, now - startTime, classifier)
        _   <- metricsOps.decreaseActiveCalls(methodInfo, classifier)
        onC <- E.delay(delegate.onClose(status, metadata))
      } yield onC
    }.unsafeRunSync
}

object MetricsChannelCallListener {
  def build[F[_]: Effect, Res](
      delegate: ClientCall.Listener[Res],
      methodInfo: GrpcMethodInfo,
      metricsOps: MetricsOps[F],
      classifier: Option[String])(implicit C: Clock[F]): F[MetricsChannelCallListener[F, Res]] =
    C.monotonic(NANOSECONDS)
      .map(new MetricsChannelCallListener[F, Res](delegate, methodInfo, metricsOps, _, classifier))
}
