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

import cats.instances.future._
import cats.~>
import io.grpc.stub.StreamObserver
import monix.eval.Callback
import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable.Operator
import monix.reactive.observers.Subscriber
import org.reactivestreams.{Subscriber => RSubscriber}

import scala.concurrent.Future

trait MonixAdapters {

  def monixSubscriber2StreamObserver: Subscriber ~> StreamObserver =
    new (Subscriber ~> StreamObserver) {

      override def apply[A](fa: Subscriber[A]): StreamObserver[A] = new StreamObserver[A] {

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

  def streamObserver2MonixSubscriber(implicit S: Scheduler): StreamObserver ~> Subscriber =
    new (StreamObserver ~> Subscriber) {

      override def apply[A](fa: StreamObserver[A]): Subscriber[A] = new Subscriber[A] {

        override implicit def scheduler: Scheduler = S
        override def onError(ex: Throwable): Unit  = fa.onError(ex)
        override def onComplete(): Unit            = fa.onCompleted()
        override def onNext(elem: A): Future[Ack] =
          catsStdInstancesForFuture(S).handleError[Ack] {
            fa.onNext(elem)
            Continue
          } { t: Throwable =>
            fa.onError(t)
            Stop
          }
      }
    }

  def streamObserver2MonixCallback: StreamObserver ~> Callback =
    new (StreamObserver ~> Callback) {

      override def apply[A](fa: StreamObserver[A]): Callback[A] = new Callback[A] {

        override def onError(ex: Throwable): Unit = fa.onError(ex)
        override def onSuccess(value: A): Unit = {
          fa.onNext(value)
          fa.onCompleted()
        }
      }
    }

}

object converters extends MonixAdapters {

  private[internal] implicit def Subscriber2StreamObserver[A](
      subscriber: Subscriber[A]): StreamObserver[A] = monixSubscriber2StreamObserver(subscriber)

  private[internal] implicit def RSubscriber2StreamObserver[A](
      rSubscriber: RSubscriber[A]): StreamObserver[A] =
    reactiveSubscriber2StreamObserver(rSubscriber)

  private[internal] implicit def StreamObserver2Subscriber[A](observer: StreamObserver[A])(
      implicit S: Scheduler): Subscriber[A] =
    streamObserver2MonixSubscriber.apply(observer)

  private[internal] implicit def StreamObserver2Callback[A](
      observer: StreamObserver[A]): Callback[A] = streamObserver2MonixCallback(observer)

  private[internal] implicit def StreamObserver2MonixOperator[Req, Res](
      op: StreamObserver[Res] => StreamObserver[Req]): Operator[Req, Res] =
    (outputSubscriber: Subscriber[Res]) => {
      implicit val s: Scheduler = outputSubscriber.scheduler

      op(outputSubscriber)
    }

}
