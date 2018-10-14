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

package mu.rpc
package internal
package client

import cats.effect.Effect
import _root_.fs2._
import _root_.fs2.interop.reactivestreams._

import scala.concurrent.ExecutionContext
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import monix.execution.Scheduler
import monix.reactive.Observable

object fs2Calls {

  def unary[F[_]: Effect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    monixCalls.unary(request, descriptor, channel, options)

  def serverStreaming[F[_]: Effect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): Stream[F, Res] =
    monixCalls
      .serverStreaming(request, descriptor, channel, options)
      .toReactivePublisher(Scheduler(EC))
      .toStream[F]

  def clientStreaming[F[_]: Effect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    monixCalls.clientStreaming(
      Observable
        .fromReactivePublisher(input.toUnicastPublisher),
      descriptor,
      channel,
      options)

  def bidiStreaming[F[_]: Effect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): Stream[F, Res] =
    monixCalls
      .bidiStreaming(
        Observable
          .fromReactivePublisher(input.toUnicastPublisher),
        descriptor,
        channel,
        options)
      .toReactivePublisher(Scheduler(EC))
      .toStream[F]
}
