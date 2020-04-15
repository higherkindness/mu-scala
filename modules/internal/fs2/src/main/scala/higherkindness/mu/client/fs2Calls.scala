/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.internal.client

import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.effect.ConcurrentEffect
import fs2.Stream
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall
import natchez.Span

object fs2Calls {

  def unary[F[_]: ConcurrentEffect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Res] =
    Fs2ClientCall[F](channel, descriptor, options)
      .flatMap(_.unaryToUnaryCall(request, headers))

  def serverStreaming[F[_]: ConcurrentEffect, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Stream[F, Res]] =
    Stream
      .eval(Fs2ClientCall[F](channel, descriptor, options))
      .flatMap(_.unaryToStreamingCall(request, headers))
      .pure[F]

  def clientStreaming[F[_]: ConcurrentEffect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Res] =
    Fs2ClientCall[F](channel, descriptor, options)
      .flatMap(_.streamingToUnaryCall(input, headers))

  def bidiStreaming[F[_]: ConcurrentEffect, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Stream[F, Res]] =
    Stream
      .eval(Fs2ClientCall[F](channel, descriptor, options))
      .flatMap(_.streamingToStreamingCall(input, headers))
      .pure[F]

  // TODO tracingServerStreaming

  def tracingClientStreaming[F[_]: ConcurrentEffect, Req, Res](
      input: Stream[Kleisli[F, Span[F], *], Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  ): Kleisli[F, Span[F], Res] =
    Kleisli[F, Span[F], Res] { parentSpan =>
      parentSpan.span(descriptor.getFullMethodName()).use { span =>
        span.kernel.flatMap { kernel =>
          val headers = tracingKernelToHeaders(kernel)
          val streamF: Stream[F, Req] =
            input.translateInterruptible(Kleisli.applyK[F, Span[F]](span))
          clientStreaming[F, Req, Res](
            streamF,
            descriptor,
            channel,
            options,
            headers
          )
        }
      }
    }

  // TODO tracingBidiStreaming

}
