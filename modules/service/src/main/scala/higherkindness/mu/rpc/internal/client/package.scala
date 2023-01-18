/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

import cats.effect.Async
import cats.syntax.all._
import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.ExecutionException
import scala.util.Try

package object client {

  private[internal] def listenableFuture2Async[F[_], A](
      lfF: F[ListenableFuture[A]]
  )(implicit F: Async[F]): F[A] =
    F.async { cb =>
      val back = lfF.flatMap { lf =>
        F.executionContext.flatMap { ec =>
          F.delay(
            lf.addListener(
              () =>
                cb(Try(lf.get).toEither.adaptError {
                  // NB: This is unwrapping the ExecutionException because the CE2 implementation also unwrapped it, via the guava `Futures.addCallback` helper
                  case ee: ExecutionException if ee.getCause != null =>
                    ee.getCause
                }),
              // Use the Async's compute EC to avoid needless context shifting across pools.
              (command: Runnable) => ec.execute(command)
            )
          )
        }
      }

      back.as(None)
    }

}
