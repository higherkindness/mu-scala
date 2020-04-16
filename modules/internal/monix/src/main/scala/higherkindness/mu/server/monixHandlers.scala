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

package higherkindness.mu.rpc.internal.server

import higherkindness.mu.rpc.protocol.CompressionType
import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, Effect}
import io.grpc._
import io.grpc.ServerCall.Listener
import io.grpc.stub.ServerCalls
import monix.execution.Scheduler
import monix.reactive.Observable
import natchez.{EntryPoint, Span}

object monixHandlers {

  // TODO check whether these context bounds are correct

  def clientStreaming[F[_]: ConcurrentEffect, Req, Res](
      f: Observable[Req] => F[Res],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    ServerCalls.asyncClientStreamingCall(
      monixMethods.clientStreamingMethod[F, Req, Res](f, compressionType)
    )

  def serverStreaming[F[_]: Effect, Req, Res](
      f: Req => F[Observable[Res]],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    ServerCalls.asyncServerStreamingCall(
      monixMethods.serverStreamingMethod[F, Req, Res](f, compressionType)
    )

  def bidiStreaming[F[_]: Effect, Req, Res](
      f: Observable[Req] => F[Observable[Res]],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    ServerCalls.asyncBidiStreamingCall(
      monixMethods.bidiStreamingMethod[F, Req, Res](f, compressionType)
    )

  def tracingClientStreaming[F[_]: ConcurrentEffect, Req, Res](
      f: Observable[Req] => Kleisli[F, Span[F], Res],
      descriptor: MethodDescriptor[Req, Res],
      entrypoint: EntryPoint[F],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    new ServerCallHandler[Req, Res] {
      def startCall(
          call: ServerCall[Req, Res],
          metadata: Metadata
      ): Listener[Req] = {
        val kernel = extractTracingKernel(metadata)
        val spanResource =
          entrypoint.continueOrElseRoot(descriptor.getFullMethodName(), kernel)

        val method = monixMethods.clientStreamingMethod[F, Req, Res](
          req => spanResource.use(span => f(req).run(span)),
          compressionType
        )

        ServerCalls.asyncClientStreamingCall(method).startCall(call, metadata)
      }
    }

  def tracingServerStreaming[F[_]: Effect, Req, Res](
      f: Req => Kleisli[F, Span[F], Observable[Res]],
      descriptor: MethodDescriptor[Req, Res],
      entrypoint: EntryPoint[F],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    new ServerCallHandler[Req, Res] {
      def startCall(
          call: ServerCall[Req, Res],
          metadata: Metadata
      ): Listener[Req] = {
        val kernel = extractTracingKernel(metadata)
        val spanResource =
          entrypoint.continueOrElseRoot(descriptor.getFullMethodName(), kernel)

        val method = monixMethods.serverStreamingMethod[F, Req, Res](
          req => spanResource.use(span => f(req).run(span)),
          compressionType
        )

        ServerCalls.asyncServerStreamingCall(method).startCall(call, metadata)
      }
    }

  def tracingBidiStreaming[F[_]: Effect, Req, Res](
      f: Observable[Req] => Kleisli[F, Span[F], Observable[Res]],
      descriptor: MethodDescriptor[Req, Res],
      entrypoint: EntryPoint[F],
      compressionType: CompressionType
  )(implicit S: Scheduler): ServerCallHandler[Req, Res] =
    new ServerCallHandler[Req, Res] {
      def startCall(
          call: ServerCall[Req, Res],
          metadata: Metadata
      ): Listener[Req] = {
        val kernel = extractTracingKernel(metadata)
        val spanResource =
          entrypoint.continueOrElseRoot(descriptor.getFullMethodName(), kernel)

        val method = monixMethods.bidiStreamingMethod[F, Req, Res](
          req => spanResource.use(span => f(req).run(span)),
          compressionType
        )

        ServerCalls.asyncBidiStreamingCall(method).startCall(call, metadata)
      }
    }

}
