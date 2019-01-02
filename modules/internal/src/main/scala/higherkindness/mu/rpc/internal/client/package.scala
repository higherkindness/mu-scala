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

package higherkindness.mu.rpc.internal

import java.util.concurrent.{Executor => JavaExecutor}

import cats.effect.Async
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.ExecutionContext

package object client {

  private[internal] def listenableFuture2Async[F[_], A](
      fa: => ListenableFuture[A])(implicit F: Async[F], EC: ExecutionContext): F[A] =
    F.async { cb =>
      Futures.addCallback(
        fa,
        new FutureCallback[A] {
          override def onSuccess(result: A): Unit = cb(Right(result))

          override def onFailure(t: Throwable): Unit = cb(Left(t))
        },
        new JavaExecutor {
          override def execute(command: Runnable): Unit = EC.execute(command)
        }
      )
    }

}
