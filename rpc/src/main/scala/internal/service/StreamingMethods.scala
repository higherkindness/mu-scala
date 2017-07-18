/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle
package rpc
package internal
package service

import cats.implicits._
import cats.{Comonad, Monad, MonadError}
import io.grpc.stub.ServerCalls.{
  BidiStreamingMethod,
  ClientStreamingMethod,
  ServerStreamingMethod,
  UnaryMethod
}
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusException}

object calls {

  private[this] def completeObserver[A](observer: StreamObserver[A])(
      t: Either[Throwable, A]): Unit = t match {
    case Right(value) =>
      observer.onNext(value)
      observer.onCompleted()
    case Left(s: StatusException) =>
      observer.onError(s)
    case Left(e) =>
      observer.onError(
        Status.INTERNAL.withDescription(e.getMessage).withCause(e).asException()
      )
  }

  def unaryMethod[F[_], M[_], Req, Res](f: (Req) => FreeS[F, Res])(
      implicit ME: MonadError[M, Throwable],
      H: FSHandler[F, M]): UnaryMethod[Req, Res] = new UnaryMethod[Req, Res] {
    override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
      val result = f(request).interpret[M]
      ME.attempt(result).map(completeObserver(responseObserver))
    }
  }

  def clientStreamingMethod[F[_], M[_], Req, Res](
      f: (StreamObserver[Res]) => FreeS[F, StreamObserver[Req]])(
      implicit ME: MonadError[M, Throwable],
      C: Comonad[M],
      H: FSHandler[F, M]): ClientStreamingMethod[Req, Res] = new ClientStreamingMethod[Req, Res] {
    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      C.extract(f(responseObserver).interpret[M])
  }

  def serverStreamingMethod[F[_], M[_], Req, Res](f: (Req, StreamObserver[Res]) => FreeS[F, Unit])(
      implicit ME: MonadError[M, Throwable],
      H: FSHandler[F, M]): ServerStreamingMethod[Req, Res] = new ServerStreamingMethod[Req, Res] {
    override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit =
      f(request, responseObserver).interpret[M]
  }

  def bidiStreamingMethod[F[_], M[_], Req, Res](
      f: (StreamObserver[Res]) => FreeS[F, StreamObserver[Req]])(
      implicit ME: MonadError[M, Throwable],
      C: Comonad[M],
      H: FSHandler[F, M]): BidiStreamingMethod[Req, Res] = new BidiStreamingMethod[Req, Res] {
    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      C.extract(f(responseObserver).interpret[M])
  }

}
