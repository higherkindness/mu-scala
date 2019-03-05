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

package higherkindness.mu.rpc.server.metrics

import cats.effect.{Clock, Effect}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc._

import scala.concurrent.duration._

case class MetricsServerInterceptor[F[_]: Clock](
    metricsOps: MetricsOps[F],
    classifier: Option[String] = None)(implicit E: Effect[F])
    extends ServerInterceptor {

  override def interceptCall[Req, Res](
      call: ServerCall[Req, Res],
      requestHeaders: Metadata,
      next: ServerCallHandler[Req, Res]): ServerCall.Listener[Req] = {

    val methodInfo: GrpcMethodInfo = GrpcMethodInfo(call.getMethodDescriptor)

    E.toIO(
        MetricsServerCall
          .build[F, Req, Res](call, methodInfo, metricsOps, classifier)
          .map(
            metricsCall =>
              MetricsServerCallListener[F, Req](
                next.startCall(metricsCall, requestHeaders),
                methodInfo,
                metricsOps,
                classifier))
      )
      .unsafeRunSync
  }
}

case class MetricsServerCall[F[_], Req, Res](
    serverCall: ServerCall[Req, Res],
    methodInfo: GrpcMethodInfo,
    metricsOps: MetricsOps[F],
    startTime: Long,
    classifier: Option[String])(implicit E: Effect[F], C: Clock[F])
    extends ForwardingServerCall.SimpleForwardingServerCall[Req, Res](serverCall) {

  override def close(status: Status, responseHeaders: Metadata): Unit =
    E.toIO {
      for {
        now <- C.monotonic(NANOSECONDS)
        _   <- metricsOps.recordTotalTime(methodInfo, status, now - startTime, classifier)
        onC <- E.delay(delegate.close(status, responseHeaders))
      } yield onC
    }.unsafeRunSync

  override def sendMessage(message: Res): Unit =
    E.toIO {
      metricsOps.recordMessageSent(methodInfo, classifier) *>
        E.delay(delegate.sendMessage(message))
    }.unsafeRunSync
}

object MetricsServerCall {
  def build[F[_]: Effect, Req, Res](
      serverCall: ServerCall[Req, Res],
      methodInfo: GrpcMethodInfo,
      metricsOps: MetricsOps[F],
      classifier: Option[String])(implicit C: Clock[F]): F[MetricsServerCall[F, Req, Res]] =
    C.monotonic(NANOSECONDS)
      .map(new MetricsServerCall[F, Req, Res](serverCall, methodInfo, metricsOps, _, classifier))
}

case class MetricsServerCallListener[F[_], Req](
    delegate: ServerCall.Listener[Req],
    methodInfo: GrpcMethodInfo,
    metricsOps: MetricsOps[F],
    classifier: Option[String])(implicit E: Effect[F])
    extends ForwardingServerCallListener[Req] {

  override def onMessage(request: Req): Unit =
    E.toIO {
      for {
        _   <- metricsOps.recordMessageReceived(methodInfo, classifier)
        _   <- metricsOps.increaseActiveCalls(methodInfo, classifier)
        onM <- E.delay(delegate.onMessage(request))
      } yield onM
    }.unsafeRunSync

  override def onComplete(): Unit =
    E.toIO {
      metricsOps.decreaseActiveCalls(methodInfo, classifier) *>
        E.delay(delegate.onComplete())
    }.unsafeRunSync
}
