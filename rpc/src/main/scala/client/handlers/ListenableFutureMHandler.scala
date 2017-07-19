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
package client.handlers

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import freestyle.async.AsyncContext

@deprecated(
  "This class will be removed from freestyle-rpc once Freestyle 0.4.0 is released, " +
    "since it'll be available as part of the Freestyle core.",
  "0.0.1")
class ListenableFutureMHandler[F[_]](implicit AC: AsyncContext[F])
    extends FSHandler[ListenableFuture, F] {
  override def apply[A](fa: ListenableFuture[A]): F[A] =
    AC.runAsync { cb =>
      Futures.addCallback(fa, new FutureCallback[A] {
        override def onSuccess(result: A): Unit = cb(Right(result))

        override def onFailure(t: Throwable): Unit = cb(Left(t))
      })
    }
}
