/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.internal.client.fs2

import cats.data.Kleisli
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all._
import fs2.Stream
import fs2.grpc.client.{ClientOptions, Fs2ClientCall}
import higherkindness.mu.rpc.internal.ClientContext
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}

object calls {

  def unary[F[_]: Async, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Res] = {
    Dispatcher[F].use { disp =>
      Fs2ClientCall[F]
        .apply(channel, descriptor, disp, clientOptions(options))
        .flatMap(_.unaryToUnaryCall(request, headers))
    }
  }

  def clientStreaming[F[_]: Async, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Res] =
    Dispatcher[F].use { disp =>
      Fs2ClientCall[F](channel, descriptor, disp, clientOptions(options))
        .flatMap(_.streamingToUnaryCall(input, headers))
    }

  def serverStreaming[F[_]: Async, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Stream[F, Res]] =
    Stream
      .resource(Dispatcher[F])
      .flatMap { disp =>
        Stream
          .eval(Fs2ClientCall[F](channel, descriptor, disp, clientOptions(options)))
          .flatMap(_.unaryToStreamingCall(request, headers))
      }
      .pure[F] // Why is this F[Stream[F, A]]? That type makes no sense.

  def bidiStreaming[F[_]: Async, Req, Res](
      input: Stream[F, Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      headers: Metadata = new Metadata()
  ): F[Stream[F, Res]] =
    Stream
      .resource(Dispatcher[F])
      .flatMap { disp =>
        Stream
          .eval(Fs2ClientCall[F](channel, descriptor, disp, clientOptions(options)))
          .flatMap(_.streamingToStreamingCall(input, headers))
      }
      .pure[F]

  def contextClientStreaming[F[_]: Async, MC, Req, Res](
      input: Stream[Kleisli[F, MC, *], Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  )(implicit C: ClientContext[F, MC]): Kleisli[F, MC, Res] =
    Kleisli[F, MC, Res] { parentSpan =>
      C[Req, Res](descriptor, channel, options, parentSpan).flatMap { case (span, metaData) =>
        val streamF: Stream[F, Req] =
          input.translate(Kleisli.applyK[F, MC](span))
        clientStreaming[F, Req, Res](
          streamF,
          descriptor,
          channel,
          options,
          metaData
        )
      }
    }

  def contextServerStreaming[F[_]: Async, MC, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  )(implicit C: ClientContext[F, MC]): Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] =
    Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] { context =>
      C[Req, Res](descriptor, channel, options, context).map { case (_, metadata) =>
        Stream
          .resource(Dispatcher[F])
          .flatMap { disp =>
            Stream
              .eval(Fs2ClientCall[F](channel, descriptor, disp, clientOptions(options)))
              .flatMap(_.unaryToStreamingCall(request, metadata))
          }
          .translate(Kleisli.liftK[F, MC])
      }
    }

  def contextBidiStreaming[F[_]: Async, MC, Req, Res](
      input: Stream[Kleisli[F, MC, *], Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  )(implicit C: ClientContext[F, MC]): Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] =
    Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] { parentContext =>
      C[Req, Res](descriptor, channel, options, parentContext).map { case (context, metadata) =>
        val streamF: Stream[F, Req] = input.translate(Kleisli.applyK[F, MC](context))
        Stream
          .resource(Dispatcher[F])
          .flatMap { disp =>
            Stream
              .eval(Fs2ClientCall[F](channel, descriptor, disp, clientOptions(options)))
              .flatMap(_.streamingToStreamingCall(streamF, metadata))
          }
          .translate(Kleisli.liftK[F, MC])
      }
    }

  // This is kind of ugly, but fixing the macro which calls the other methods in here would be complex,
  // since it also handles monix etc, not just fs2-grpc
  private def clientOptions(options: CallOptions): ClientOptions =
    ClientOptions.default.configureCallOptions(_ => options)
}
