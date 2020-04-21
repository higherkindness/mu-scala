/*
 * Copyright 2017-2020 47 Degrees <http://47deg.com>
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

package higherkindness.mu.rpc.internal.client.monix

import monix.reactive.Observable
import monix.execution.Scheduler
import monix.execution.rstreams.Subscription

import cats.effect.{Async, Sync}
import cats.Applicative
import cats.data.Kleisli
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import higherkindness.mu.rpc.internal.client._
import io.grpc.stub.{ClientCalls, StreamObserver}
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import natchez.Span
import org.reactivestreams.{Publisher, Subscriber}

object calls {

  import higherkindness.mu.rpc.internal.converters._

  def clientStreaming[F[_]: Async, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata = new Metadata()
  )(implicit S: Scheduler): F[Res] =
    input
      .liftByOperator(
        StreamObserver2MonixOperator((outputObserver: StreamObserver[Res]) =>
          ClientCalls.asyncClientStreamingCall(
            new HeaderAttachingClientCall(channel.newCall(descriptor, options), extraHeaders),
            outputObserver
          )
        )
      )
      .firstL
      .toAsync[F]

  def serverStreaming[F[_]: Applicative, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata = new Metadata()
  ): F[Observable[Res]] =
    _serverStreaming[Req, Res](request, descriptor, channel, options, extraHeaders)
      .pure[F]

  def bidiStreaming[F[_]: Applicative, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata = new Metadata()
  ): F[Observable[Res]] =
    _bidiStreaming[Req, Res](input, descriptor, channel, options, extraHeaders)
      .pure[F]

  def tracingClientStreaming[F[_]: Async, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  )(implicit S: Scheduler): Kleisli[F, Span[F], Res] =
    Kleisli[F, Span[F], Res] { parentSpan =>
      parentSpan.span(descriptor.getFullMethodName()).use { span =>
        span.kernel.flatMap { kernel =>
          val headers = tracingKernelToHeaders(kernel)
          clientStreaming[F, Req, Res](input, descriptor, channel, options, headers)
        }
      }
    }

  def tracingServerStreaming[F[_]: Sync, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  ): Kleisli[F, Span[F], Observable[Res]] =
    Kleisli[F, Span[F], Observable[Res]] { parentSpan =>
      parentSpan.span(descriptor.getFullMethodName()).use { span =>
        span.kernel.map { kernel =>
          val headers = tracingKernelToHeaders(kernel)
          _serverStreaming[Req, Res](request, descriptor, channel, options, headers)
        }
      }
    }

  def tracingBidiStreaming[F[_]: Sync, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions
  ): Kleisli[F, Span[F], Observable[Res]] =
    Kleisli[F, Span[F], Observable[Res]] { parentSpan =>
      parentSpan.span(descriptor.getFullMethodName()).use { span =>
        span.kernel.map { kernel =>
          val headers = tracingKernelToHeaders(kernel)
          _bidiStreaming[Req, Res](input, descriptor, channel, options, headers)
        }
      }
    }

  private def _serverStreaming[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata
  ): Observable[Res] =
    Observable.fromReactivePublisher(
      createPublisher(request, descriptor, channel, options, extraHeaders)
    )

  private def _bidiStreaming[Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata
  ): Observable[Res] =
    input.liftByOperator(
      StreamObserver2MonixOperator((outputObserver: StreamObserver[Res]) =>
        ClientCalls.asyncBidiStreamingCall(
          new HeaderAttachingClientCall(channel.newCall(descriptor, options), extraHeaders),
          outputObserver
        )
      )
    )

  private[this] def createPublisher[Res, Req](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      extraHeaders: Metadata
  ): Publisher[Res] = {
    new Publisher[Res] {
      override def subscribe(s: Subscriber[_ >: Res]): Unit = {
        val subscriber: Subscriber[Res] = s.asInstanceOf[Subscriber[Res]]
        s.onSubscribe(Subscription.empty)
        ClientCalls.asyncServerStreamingCall(
          new HeaderAttachingClientCall(channel.newCall(descriptor, options), extraHeaders),
          request,
          subscriber.toStreamObserver
        )
      }
    }
  }

}
