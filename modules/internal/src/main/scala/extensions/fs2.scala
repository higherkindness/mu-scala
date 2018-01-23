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

package freestyle
package rpc
package internal
package extensions

import cats.~>
import io.grpc.stub.StreamObserver
import _root_.fs2.interop.reactivestreams._
import org.reactivestreams.{Subscriber => RSubscriber}
import cats.effect.Effect

object fs2 {

  final class Adapters[F[_]: Effect] {

    type FStreamSubscriber[A] = StreamSubscriber[F, A]

    def fs2Subscriber2StreamObserver: FStreamSubscriber ~> StreamObserver =
      new (FStreamSubscriber ~> StreamObserver) {

        override def apply[A](fa: FStreamSubscriber[A]): StreamObserver[A] = new StreamObserver[A] {

          override def onError(t: Throwable): Unit = fa.onError(t)
          override def onCompleted(): Unit         = fa.onComplete()
          override def onNext(value: A): Unit = {
            fa.onNext(value)
            (): Unit
          }
        }

      }

    def reactiveSubscriber2StreamObserver: RSubscriber ~> StreamObserver =
      new (RSubscriber ~> StreamObserver) {

        override def apply[A](fa: RSubscriber[A]): StreamObserver[A] = new StreamObserver[A] {

          override def onError(t: Throwable): Unit = fa.onError(t)
          override def onCompleted(): Unit         = fa.onComplete()
          override def onNext(value: A): Unit      = fa.onNext(value)
        }
      }

    object implicits {

      private[internal] implicit def Subscriber2StreamObserver[A](
          subscriber: FStreamSubscriber[A]): StreamObserver[A] =
        fs2Subscriber2StreamObserver(subscriber)

      private[internal] implicit def RSubscriber2StreamObserver[A](
          rSubscriber: RSubscriber[A]): StreamObserver[A] =
        reactiveSubscriber2StreamObserver(rSubscriber)

    }

  }

  def apply[F[_]: Effect]: Adapters[F] = new Adapters[F]
}
