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
package server

import cats.effect.{Effect, IO}
import cats.syntax.functor._
import io.grpc.stub.ServerCalls.{
  BidiStreamingMethod,
  ClientStreamingMethod,
  ServerStreamingMethod,
  UnaryMethod
}
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusException}
import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}
import monix.reactive.observers.Subscriber

import scala.concurrent.Future

object calls {

  import freestyle.rpc.internal.extensions.monix.implicits._

  def unaryMethod[F[_], Req, Res](f: Req => F[Res])(
      implicit EFF: Effect[F]): UnaryMethod[Req, Res] =
    new UnaryMethod[Req, Res] {
      override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit =
        EFF
          .runAsync(f(request))(either => IO(completeObserver(responseObserver)(either)))
          .unsafeRunAsync(_ => ())
    }

  def clientStreamingMethod[F[_], Req, Res](f: Observable[Req] => F[Res])(
      implicit EFF: Effect[F],
      S: Scheduler): ClientStreamingMethod[Req, Res] = new ClientStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      transform[Req, Res](
        inputObservable => Observable.fromEffect(f(inputObservable)),
        responseObserver
      )
  }

  def serverStreamingMethod[F[_], Req, Res](f: Req => F[Observable[Res]])(
      implicit EFF: Effect[F],
      S: Scheduler): ServerStreamingMethod[Req, Res] = new ServerStreamingMethod[Req, Res] {

    override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit =
      EFF
        .runAsync(f(request)) {
          case Right(obs) => IO(obs.subscribe(responseObserver)).void
          case Left(e)    => IO.raiseError(e) // this will throw, but consistent previous impl
        }
        .unsafeRunSync
  }

  def bidiStreamingMethod[F[_], Req, Res](f: Observable[Req] => F[Observable[Res]])(
      implicit EFF: Effect[F],
      S: Scheduler): BidiStreamingMethod[Req, Res] = new BidiStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      Subscriber2StreamObserver {
        transform[Req, Res](
          (inputObservable: Observable[Req]) => Observable.fromEffect(f(inputObservable)).flatten,
          StreamObserver2Subscriber(responseObserver)
        )
      }
  }

  private[this] def completeObserver[A](observer: StreamObserver[A]): Either[Throwable, A] => Unit = {
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

  private[this] def transform[Req, Res](
      transformer: Observable[Req] => Observable[Res],
      subscriber: Subscriber[Res]): Subscriber[Req] =
    new Subscriber[Req] {

      val pipe: Pipe[Req, Res]                      = Pipe.publish[Req].transform[Res](transformer)
      val (in: Observer[Req], out: Observable[Res]) = pipe.unicast

      out.unsafeSubscribeFn(subscriber)

      override implicit def scheduler: Scheduler   = subscriber.scheduler
      override def onError(t: Throwable): Unit     = in.onError(t)
      override def onComplete(): Unit              = in.onComplete()
      override def onNext(value: Req): Future[Ack] = in.onNext(value)
    }

}
