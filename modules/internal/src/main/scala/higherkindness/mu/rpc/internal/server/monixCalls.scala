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
package server

import cats.effect.{ConcurrentEffect, Effect, IO}
import io.grpc.stub.ServerCalls.{
  BidiStreamingMethod,
  ClientStreamingMethod,
  ServerStreamingMethod,
  UnaryMethod
}
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusException, StatusRuntimeException}

import scala.concurrent.ExecutionContext
import monix.reactive.Observable

object monixCalls {

  import higherkindness.mu.rpc.internal.converters._

  def unaryMethod[F[_]: Effect, Req, Res](
      f: Req => F[Res],
      maybeCompression: Option[String]): UnaryMethod[Req, Res] =
    new UnaryMethod[Req, Res] {
      override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
        addCompression(responseObserver, maybeCompression)
        Effect[F]
          .runAsync(f(request))(either => IO(completeObserver(responseObserver)(either)))
          .toIO
          .unsafeRunAsync(_ => ())
      }
    }

  def clientStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Observable[Req] => F[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ClientStreamingMethod[Req, Res] =
    new ClientStreamingMethod[Req, Res] {

      override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
        addCompression(responseObserver, maybeCompression)
        transformStreamObserver[Req, Res](
          inputObservable => Observable.from(f(inputObservable)),
          responseObserver
        )
      }
    }

  def serverStreamingMethod[F[_]: Effect, Req, Res](
      f: Req => Observable[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerStreamingMethod[Req, Res] =
    new ServerStreamingMethod[Req, Res] {

      override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
        addCompression(responseObserver, maybeCompression)
        f(request).subscribe(responseObserver.toSubscriber)
        ()
      }
    }

  def bidiStreamingMethod[F[_]: Effect, Req, Res](
      f: Observable[Req] => Observable[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): BidiStreamingMethod[Req, Res] =
    new BidiStreamingMethod[Req, Res] {

      override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
        addCompression(responseObserver, maybeCompression)
        transformStreamObserver(inputObservable => f(inputObservable), responseObserver)
      }
    }

  private[this] def completeObserver[A](observer: StreamObserver[A]): Either[Throwable, A] => Unit = {
    case Right(value) =>
      observer.onNext(value)
      observer.onCompleted()
    case Left(s: StatusException) =>
      observer.onError(s)
    case Left(s: StatusRuntimeException) =>
      observer.onError(s)
    case Left(e) =>
      observer.onError(
        Status.INTERNAL.withDescription(e.getMessage).withCause(e).asException()
      )
  }

}
