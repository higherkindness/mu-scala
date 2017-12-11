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

import freestyle.free._
import freestyle.async.AsyncContext
import monix.eval.{Callback, Task}
import monix.execution.Scheduler

class TaskMHandler[F[_]](implicit AC: AsyncContext[F], S: Scheduler) extends FSHandler[Task, F] {

  override def apply[A](fa: Task[A]): F[A] = AC.runAsync { cb =>
    fa.runAsync(new Callback[A] {
      override def onSuccess(value: A): Unit = cb(Right(value))

      override def onError(ex: Throwable): Unit = cb(Left(ex))
    })

  }
}
