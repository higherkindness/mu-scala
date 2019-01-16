/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.effect.{ConcurrentEffect, Effect}
import io.grpc.stub.ServerCalls.{BidiStreamingMethod, ClientStreamingMethod, ServerStreamingMethod}
import io.grpc.stub.StreamObserver
import monix.execution.{Ack, Scheduler}
import monix.reactive.observers.Subscriber

import scala.concurrent.{ExecutionContext, Future}
import monix.reactive.{Observable, Observer, Pipe}

object monixCalls {

  import higherkindness.mu.rpc.internal.converters._

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

  private[this] def transformStreamObserver[Req, Res](
      transformer: Observable[Req] => Observable[Res],
      responseObserver: StreamObserver[Res]
  )(implicit EC: ExecutionContext): StreamObserver[Req] =
    transform(transformer, responseObserver.toSubscriber).toStreamObserver

}
