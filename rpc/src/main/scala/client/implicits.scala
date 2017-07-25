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
package client

import com.google.common.util.concurrent.ListenableFuture
import freestyle.async.AsyncContext
import freestyle.rpc.client.handlers._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future

trait Conversions {

  implicit def listenableFutureHandler[F[_]](
      implicit AC: AsyncContext[F]): ListenableFutureMHandler[F] =
    new ListenableFutureMHandler[F]

  implicit def listenableFutureToAsyncConverter[F[_], A](future: ListenableFuture[A])(
      implicit AC: AsyncContext[F]): F[A] =
    listenableFutureHandler.apply(future)
}

trait FutureInstances {

  implicit def task2Future(
      implicit AC: AsyncContext[Future],
      S: Scheduler): FSHandler[Task, Future] =
    new TaskMHandler[Future]

}

object implicits extends CaptureInstances with Conversions with FutureInstances
