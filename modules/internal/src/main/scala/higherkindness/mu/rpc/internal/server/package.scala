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

import io.grpc.stub.{ServerCallStreamObserver, StreamObserver}
import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}
import monix.reactive.observers.Subscriber

import scala.concurrent.{ExecutionContext, Future}

package object server {

  private[server] def transform[Req, Res](
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

  import higherkindness.mu.rpc.internal.converters._

  private[server] def transformStreamObserver[Req, Res](
      transformer: Observable[Req] => Observable[Res],
      responseObserver: StreamObserver[Res]
  )(implicit EC: ExecutionContext): StreamObserver[Req] =
    transform(transformer, responseObserver.toSubscriber).toStreamObserver

  private[server] def addCompression[A](
      observer: StreamObserver[A],
      algorithm: Option[String]): Unit =
    (observer, algorithm) match {
      case (o: ServerCallStreamObserver[_], Some(alg)) => o.setCompression(alg)
      case _                                           =>
    }

}
