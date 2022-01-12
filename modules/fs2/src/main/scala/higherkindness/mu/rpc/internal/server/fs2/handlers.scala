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

package higherkindness.mu.rpc.internal.server.fs2

import cats.data.Kleisli
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.functor._
import fs2.Stream
import fs2.grpc.server.{Fs2ServerCallHandler, GzipCompressor, ServerOptions}
import higherkindness.mu.rpc.internal.ServerContext
import higherkindness.mu.rpc.protocol.{CompressionType, Gzip, Identity}
import io.grpc.{Metadata, MethodDescriptor, ServerCallHandler}

object handlers {

  private def serverCallOptions(compressionType: CompressionType): ServerOptions =
    compressionType match {
      case Identity => ServerOptions.default
      case Gzip =>
        ServerOptions.default.configureCallOptions(_.withServerCompressor(Some(GzipCompressor)))
    }

  // Note: this handler is never actually used anywhere. Delete?
  def unary[F[_]: Async, Req, Res](
      f: (Req, Metadata) => F[Res],
      disp: Dispatcher[F],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F](disp, serverCallOptions(compressionType)).unaryToUnaryCall[Req, Res](
      f
    )

  def clientStreaming[F[_]: Async, Req, Res](
      f: (Stream[F, Req], Metadata) => F[Res],
      disp: Dispatcher[F],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F](disp, serverCallOptions(compressionType))
      .streamingToUnaryCall[Req, Res](f)

  def serverStreaming[F[_]: Async, Req, Res](
      f: (Req, Metadata) => F[Stream[F, Res]],
      disp: Dispatcher[F],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F](disp, serverCallOptions(compressionType))
      .unaryToStreamingCall[Req, Res] { (req, metadata) =>
        Stream.force(f(req, metadata))
      }

  def bidiStreaming[F[_]: Async, Req, Res](
      f: (Stream[F, Req], Metadata) => F[Stream[F, Res]],
      disp: Dispatcher[F],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F](disp, serverCallOptions(compressionType))
      .streamingToStreamingCall[Req, Res]((stream, metadata) => Stream.force(f(stream, metadata)))

  def contextClientStreaming[F[_]: Async, MC, Req, Res](
      f: Stream[Kleisli[F, MC, *], Req] => Kleisli[F, MC, Res],
      descriptor: MethodDescriptor[Req, Res],
      disp: Dispatcher[F],
      compressionType: CompressionType
  )(implicit C: ServerContext[F, MC]): ServerCallHandler[Req, Res] =
    clientStreaming[F, Req, Res](
      { (req: Stream[F, Req], metadata: Metadata) =>
        val streamK: Stream[Kleisli[F, MC, *], Req] = req.translate(Kleisli.liftK[F, MC])
        C[Req, Res](descriptor, metadata).use[Res] { span =>
          f(streamK).run(span)
        }
      },
      disp,
      compressionType
    )

  def contextServerStreaming[F[_]: Async, MC, Req, Res](
      f: Req => Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]],
      descriptor: MethodDescriptor[Req, Res],
      disp: Dispatcher[F],
      compressionType: CompressionType
  )(implicit C: ServerContext[F, MC]): ServerCallHandler[Req, Res] =
    serverStreaming[F, Req, Res](
      { (req: Req, metadata: Metadata) =>
        C[Req, Res](descriptor, metadata)
          .use[Stream[F, Res]] { context =>
            val kleisli: Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] = f(req)
            val fStreamK: F[Stream[Kleisli[F, MC, *], Res]] = kleisli.run(context)
            fStreamK.map(_.translate(Kleisli.applyK[F, MC](context)))
          }
      },
      disp,
      compressionType
    )

  def contextBidiStreaming[F[_]: Async, MC, Req, Res](
      f: Stream[Kleisli[F, MC, *], Req] => Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]],
      descriptor: MethodDescriptor[Req, Res],
      disp: Dispatcher[F],
      compressionType: CompressionType
  )(implicit C: ServerContext[F, MC]): ServerCallHandler[Req, Res] =
    bidiStreaming[F, Req, Res](
      { (req: Stream[F, Req], metadata: Metadata) =>
        val reqStreamK: Stream[Kleisli[F, MC, *], Req] =
          req.translate(Kleisli.liftK[F, MC])
        C[Req, Res](descriptor, metadata)
          .use[Stream[F, Res]] { context =>
            val kleisli: Kleisli[F, MC, Stream[Kleisli[F, MC, *], Res]] = f(reqStreamK)
            val fStreamK: F[Stream[Kleisli[F, MC, *], Res]] = kleisli.run(context)
            fStreamK.map(_.translate(Kleisli.applyK[F, MC](context)))
          }
      },
      disp,
      compressionType
    )

}
