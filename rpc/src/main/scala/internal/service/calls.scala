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
import cats.{Comonad, MonadError}
import io.grpc.stub.ServerCalls.{
  BidiStreamingMethod,
  ClientStreamingMethod,
  ServerStreamingMethod,
  UnaryMethod
}
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusException}
import monix.eval.Task
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable
import monix.reactive.observables.ObservableLike.Transformer
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.PublishSubject

import scala.concurrent.Future

object calls {

  import converters._

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
      (): Unit
    }
  }

  def clientStreamingMethod[F[_], M[_], Req, Res](f: (Observable[Req]) => FreeS[F, Res])(
      implicit ME: MonadError[M, Throwable],
      H: FSHandler[F, M],
      HTask: FSHandler[M, Task],
      S: Scheduler): ClientStreamingMethod[Req, Res] = new ClientStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
      transform[Req, Res](
        inputObservable => Observable.fromTask(HTask(f(inputObservable).interpret[M])),
        responseObserver
      )
    }
  }

  def serverStreamingMethod[F[_], M[_], Req, Res](f: (Req) => FreeS[F, Observable[Res]])(
      implicit ME: MonadError[M, Throwable],
      C: Comonad[M],
      H: FSHandler[F, M],
      S: Scheduler): ServerStreamingMethod[Req, Res] = new ServerStreamingMethod[Req, Res] {

    override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
      C.extract(f(request).interpret[M]).subscribe(responseObserver)
      (): Unit
    }
  }

  def bidiStreamingMethod[F[_], M[_], Req, Res](f: (Observable[Req]) => FreeS[F, Observable[Res]])(
      implicit ME: MonadError[M, Throwable],
      C: Comonad[M],
      H: FSHandler[F, M],
      S: Scheduler): BidiStreamingMethod[Req, Res] = new BidiStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
      transform[Req, Res](
        inputObservable => C.extract(f(inputObservable).interpret[M]),
        StreamObserver2Subscriber(responseObserver)
      )
    }
  }

  private[this] def transform[Req, Res](
      transformer: Transformer[Req, Res],
      subscriber: Subscriber[Res]): Subscriber[Req] =
    new Subscriber[Req] {

      val subject: PublishSubject[Req] = PublishSubject[Req]
      subject.transform(transformer).subscribe(subscriber)

      override implicit def scheduler: Scheduler   = subscriber.scheduler
      override def onError(t: Throwable): Unit     = subject.onError(t)
      override def onComplete(): Unit              = subject.onComplete()
      override def onNext(value: Req): Future[Ack] = subject.onNext(value)
    }

}
