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

package freestyle.rpc.http

import cats.effect.Effect
import fs2.Stream
import fs2.interop.reactivestreams._
import monix.execution.Scheduler
import monix.reactive.Observable
import scala.concurrent.ExecutionContext

object Util {

  implicit class Fs2StreamOps[F[_], A](stream: Stream[F, A]) {
    def toObservable(implicit F: Effect[F], ec: ExecutionContext): Observable[A] =
      Observable.fromReactivePublisher(stream.toUnicastPublisher)
  }

  implicit class MonixStreamOps[A](stream: Observable[A]) {
    def toStream[F[_]](implicit F: Effect[F], sc: Scheduler): Stream[F, A] =
      stream.toReactivePublisher.toStream[F]
  }

}
