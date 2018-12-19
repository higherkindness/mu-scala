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

import cats.effect.ConcurrentEffect
import cats.syntax.flatMap._
import fs2.Stream
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall

import scala.concurrent.ExecutionContext

object fs2Calls {

  def unary[F[_]: ConcurrentEffect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    Fs2ClientCall[F](channel, descriptor, options)
      .flatMap(_.unaryToUnaryCall(request, new Metadata()))

  def serverStreaming[F[_]: ConcurrentEffect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): Stream[F, Res] =
    Stream
      .eval(Fs2ClientCall[F](channel, descriptor, options))
      .flatMap(_.unaryToStreamingCall(request, new Metadata()))

  def clientStreaming[F[_]: ConcurrentEffect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): F[Res] =
    Fs2ClientCall[F](channel, descriptor, options)
      .flatMap(_.streamingToUnaryCall(input, new Metadata()))

  def bidiStreaming[F[_]: ConcurrentEffect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit EC: ExecutionContext): Stream[F, Res] =
    Stream
      .eval(Fs2ClientCall[F](channel, descriptor, options))
      .flatMap(_.streamingToStreamingCall(input, new Metadata()))
}
