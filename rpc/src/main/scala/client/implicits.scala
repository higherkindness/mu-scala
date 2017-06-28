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

package freestyle.rpc
package client

import cats.~>
import com.google.common.util.concurrent._
import freestyle._
import freestyle.implicits._
import freestyle.async.AsyncContext

trait Conversions {

  class ListenableFuture2AsyncM[F[_]](implicit AC: AsyncContext[F])
      extends FSHandler[ListenableFuture, F] {
    override def apply[A](fa: ListenableFuture[A]): F[A] =
      AC.runAsync { cb =>
        Futures.addCallback(fa, new FutureCallback[A] {
          override def onSuccess(result: A): Unit = cb(Right(result))

          override def onFailure(t: Throwable): Unit = cb(Left(t))
        })
      }
  }

  implicit def listenableFuture2Async[F[_]](implicit AC: AsyncContext[F]): ListenableFuture ~> F =
    new ListenableFuture2AsyncM[F]
}

object implicits extends CaptureInstances with Conversions
