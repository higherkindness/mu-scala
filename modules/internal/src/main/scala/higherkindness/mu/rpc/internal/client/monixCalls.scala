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
package internal
package client

import java.util.concurrent.{Executor => JavaExecutor}

import cats.effect.Async
import com.google.common.util.concurrent._
import io.grpc.stub.{ClientCalls, StreamObserver}
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import monix.reactive.Observable
import higherkindness.mu.rpc.internal.task._

import scala.concurrent.ExecutionContext

object monixCalls {

  import higherkindness.mu.rpc.internal.converters._

  def unary[F[_]: Async, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    listenableFuture2Async[F, Res](
      ClientCalls
        .futureUnaryCall(channel.newCall(descriptor, options), request))

  def serverStreaming[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    Observable
      .fromReactivePublisher(createPublisher(request, descriptor, channel, options))

  def clientStreaming[F[_]: Async, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    input
      .liftByOperator(
        StreamObserver2MonixOperator(
          (outputObserver: StreamObserver[Res]) =>
            ClientCalls.asyncClientStreamingCall(
              channel.newCall(descriptor, options),
              outputObserver
          )
        )
      )
      .firstL
      .toAsync[F]

  def bidiStreaming[Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    input.liftByOperator(
      StreamObserver2MonixOperator(
        (outputObserver: StreamObserver[Res]) =>
          ClientCalls.asyncBidiStreamingCall(
            channel.newCall(descriptor, options),
            outputObserver
        ))
    )

  private[this] def listenableFuture2Async[F[_], A](
      fa: => ListenableFuture[A])(implicit F: Async[F], EC: ExecutionContext): F[A] =
    F.async { cb =>
      Futures.addCallback(
        fa,
        new FutureCallback[A] {
          override def onSuccess(result: A): Unit = cb(Right(result))

          override def onFailure(t: Throwable): Unit = cb(Left(t))
        },
        new JavaExecutor {
          override def execute(command: Runnable): Unit = EC.execute(command)
        }
      )
    }
}
