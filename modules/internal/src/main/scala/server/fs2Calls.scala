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

import _root_.fs2.Stream
import _root_.fs2.interop.reactivestreams._
import cats.effect.Effect
import io.grpc.stub.ServerCalls._
import io.grpc.stub.StreamObserver
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext
import monix.reactive.Observable

object fs2Calls {

  import freestyle.rpc.internal.converters._

  def unaryMethod[F[_]: Effect, Req, Res](
      f: Req => F[Res],
      maybeCompression: Option[String]): UnaryMethod[Req, Res] =
    monixCalls.unaryMethod(f, maybeCompression)

  def clientStreamingMethod[F[_]: Effect, Req, Res](
      f: Stream[F, Req] => F[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ClientStreamingMethod[Req, Res] =
    new ClientStreamingMethod[Req, Res] {

      override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
        addCompression(responseObserver, maybeCompression)
        transformStreamObserver[Req, Res](
          inputObservable =>
            Observable.fromEffect(
              f(inputObservable.toReactivePublisher(Scheduler(EC)).toStream[F])),
          responseObserver
        )
      }
    }

  def serverStreamingMethod[F[_]: Effect, Req, Res](
      f: Req => Stream[F, Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerStreamingMethod[Req, Res] =
    new ServerStreamingMethod[Req, Res] {

      override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
        addCompression(responseObserver, maybeCompression)
        f(request).toUnicastPublisher.subscribe(responseObserver.toSubscriber.toReactive)
      }
    }

  def bidiStreamingMethod[F[_]: Effect, Req, Res](
      f: Stream[F, Req] => Stream[F, Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): BidiStreamingMethod[Req, Res] =
    new BidiStreamingMethod[Req, Res] {

      override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] = {
        addCompression(responseObserver, maybeCompression)
        transformStreamObserver[Req, Res](
          (inputObservable: Observable[Req]) =>
            Observable.fromReactivePublisher(
              f(inputObservable.toReactivePublisher(Scheduler(EC)).toStream[F]).toUnicastPublisher),
          responseObserver
        )
      }
    }

}
