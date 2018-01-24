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
import cats.effect.{Effect, IO}
import cats.syntax.functor._
import io.grpc.stub.ServerCalls._
import io.grpc.stub.StreamObserver
import monix.execution.Scheduler
import monix.reactive.Observable

object fs2Calls {

  import freestyle.rpc.internal.converters._

  def unaryMethod[F[_], Req, Res](f: Req => F[Res])(
      implicit EFF: Effect[F]): UnaryMethod[Req, Res] =
    monixCalls.unaryMethod(f)

  def clientStreamingMethod[F[_], Req, Res](f: Stream[F, Req] => F[Res])(
      implicit EFF: Effect[F],
      S: Scheduler): ClientStreamingMethod[Req, Res] = new ClientStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      monixCalls.transform[Req, Res](
        inputObservable =>
          Observable.fromEffect(f(inputObservable.toReactivePublisher.toStream[F])),
        responseObserver
      )
  }

  def serverStreamingMethod[F[_], Req, Res](f: Req => F[Stream[F, Res]])(
      implicit EFF: Effect[F],
      S: Scheduler): ServerStreamingMethod[Req, Res] = new ServerStreamingMethod[Req, Res] {

    override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit =
      EFF
        .runAsync(f(request)) {
          case Right(stream) =>
            println("1")

            IO {
              println("2")
              Observable
                .fromReactivePublisher(stream.toUnicastPublisher)
                .subscribe(responseObserver)
              println("3")
            }.void
          case Left(e) => IO.raiseError(e) // this will throw, but consistent previous impl
        }
        .unsafeRunSync
  }

  def bidiStreamingMethod[F[_], Req, Res](f: Stream[F, Req] => F[Stream[F, Res]])(
      implicit EFF: Effect[F],
      S: Scheduler): BidiStreamingMethod[Req, Res] = new BidiStreamingMethod[Req, Res] {

    override def invoke(responseObserver: StreamObserver[Res]): StreamObserver[Req] =
      Subscriber2StreamObserver {
        monixCalls.transform[Req, Res](
          (inputObservable: Observable[Req]) => {
            Observable
              .fromEffect(
                f(inputObservable.toReactivePublisher.toStream[F])
                  .map(ss => Observable.fromReactivePublisher(ss.toUnicastPublisher()))
              )
          }.flatten,
          StreamObserver2Subscriber(responseObserver)
        )
      }
  }

}
