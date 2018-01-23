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
package internal
package client

import cats.effect.Effect
import freestyle.async.guava.implicits._
import freestyle.async.catsEffect.implicits._
import cats.effect._
import _root_.fs2._
import _root_.fs2.interop.reactivestreams._
import cats.Monoid
import monix.execution.Scheduler
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import io.grpc.stub.{ClientCalls, StreamObserver}
import org.reactivestreams._

class fs2Calls[F[_]: Effect] {

  private[this] val fs2Adapters = freestyle.rpc.internal.extensions.fs2[F]
  import fs2Adapters.implicits._

  def unary[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit S: Scheduler): F[Res] =
    listenableFuture2Async(
      ClientCalls
        .futureUnaryCall(channel.newCall(descriptor, options), request))

  def serverStreaming[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit S: Scheduler): Stream[F, Res] =
    new Publisher[Res] {
      override def subscribe(s: Subscriber[_ >: Res]): Unit = {
        val subscriber: Subscriber[Res] = s.asInstanceOf[Subscriber[Res]]
        ClientCalls.asyncServerStreamingCall(
          channel.newCall[Req, Res](descriptor, options),
          request,
          subscriber
        )
      }
    }.toStream[F]

  def clientStreaming[Req, Res: Monoid](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit S: Scheduler): F[Res] = {
    val r: StreamObserver[Res] => StreamObserver[Req] = {
      (outputStreamObserver: StreamObserver[Res]) =>
        ClientCalls.asyncClientStreamingCall(
          channel.newCall(descriptor, options),
          outputStreamObserver)
    }
    val subscriber: fs2Adapters.FStreamSubscriber[Res] = ???
    subscriber.stream.compile.foldMonoid
  }

  def bidiStreaming[Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Stream[F, Res] = ???
}
